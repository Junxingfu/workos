#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

use std::{
    fs::{self, OpenOptions},
    path::{Path, PathBuf},
    process::{Child, Command, Stdio},
    sync::{
        atomic::{AtomicBool, AtomicU16, Ordering},
        Arc, Mutex,
    },
    thread,
    time::{Duration, Instant},
};
use tauri::{
    menu::{Menu, MenuItem, Submenu},
    tray::TrayIconBuilder,
    Manager, WebviewUrl, WebviewWindowBuilder,
};

#[derive(Default)]
struct Engine {
    child: Mutex<Option<Child>>,
    stopping: AtomicBool,
    port: AtomicU16,
}
fn reveal(app: &tauri::AppHandle) {
    if let Some(window) = app.get_webview_window("main") {
        let _ = window.show();
        let _ = window.unminimize();
        let _ = window.set_focus();
    }
}
fn stop(engine: &Engine) {
    engine.stopping.store(true, Ordering::SeqCst);
    if let Some(mut child) = engine.child.lock().unwrap().take() {
        unsafe {
            libc::kill(child.id() as i32, libc::SIGTERM);
        }
        for _ in 0..50 {
            if matches!(child.try_wait(), Ok(Some(_))) {
                return;
            }
            thread::sleep(Duration::from_millis(100));
        }
        let _ = child.kill();
        let _ = child.wait();
    }
}
fn launch(resources: &Path, data: &Path, token: &str) -> Result<Child, String> {
    let log_path = data.join("desktop.log");
    if fs::metadata(&log_path)
        .map(|m| m.len() > 5_000_000)
        .unwrap_or(false)
    {
        let _ = fs::rename(&log_path, data.join("desktop.previous.log"));
    }
    let log = OpenOptions::new()
        .create(true)
        .append(true)
        .open(log_path)
        .map_err(|e| e.to_string())?;
    let _ = fs::remove_file(data.join("ready.json"));
    Command::new(resources.join("runtime/bin/java"))
        .args(["-Xms64m", "-Xmx384m", "-Dfile.encoding=UTF-8", "-jar"])
        .arg(resources.join("workos-server.jar"))
        .args([
            "--spring.profiles.active=desktop",
            "--server.port=0",
            "--server.address=127.0.0.1",
        ])
        .env("WORKOS_DATA_DIR", data)
        .env("WORKOS_DESKTOP_TOKEN", token)
        .env("WORKOS_PARENT_PID", std::process::id().to_string())
        .env_remove("SPRING_DATASOURCE_URL")
        .env_remove("JAVA_TOOL_OPTIONS")
        .env_remove("JDK_JAVA_OPTIONS")
        .current_dir(data)
        .stdin(Stdio::null())
        .stdout(log.try_clone().map_err(|e| e.to_string())?)
        .stderr(log)
        .spawn()
        .map_err(|e| format!("无法启动内置运行环境：{e}"))
}
fn show_message(app: &tauri::AppHandle, message: &str) {
    if let Some(window) = app.get_webview_window("main") {
        let js = format!("document.getElementById('detail') && (document.getElementById('detail').textContent = {})", serde_json::to_string(message).unwrap());
        let _ = window.eval(js);
    }
}
fn supervise(app: tauri::AppHandle, engine: Arc<Engine>, resources: PathBuf, data: PathBuf) {
    let welcome = if data.join("workos.db").exists() {
        "0"
    } else {
        "1"
    };
    // 每次子进程启动生成不同会话密钥。后端只监听 OS 分配的回环端口。
    for attempt in 0..4 {
        if engine.stopping.load(Ordering::SeqCst) {
            return;
        }
        let token = uuid::Uuid::new_v4().simple().to_string();
        let child = match launch(&resources, &data, &token) {
            Ok(child) => child,
            Err(error) => {
                show_message(&app, &error);
                return;
            }
        };
        {
            let mut slot = engine.child.lock().unwrap();
            if engine.stopping.load(Ordering::SeqCst) {
                let mut child = child;
                let _ = child.kill();
                let _ = child.wait();
                return;
            }
            *slot = Some(child);
        }
        let mut opened = false;
        let started = Instant::now();
        loop {
            thread::sleep(Duration::from_millis(500));
            if engine.stopping.load(Ordering::SeqCst) {
                return;
            }
            let exited = engine
                .child
                .lock()
                .unwrap()
                .as_mut()
                .map(|c| !matches!(c.try_wait(), Ok(None)))
                .unwrap_or(true);
            if exited {
                engine.child.lock().unwrap().take();
                break;
            }
            if !opened {
                if let Ok(json) = fs::read_to_string(data.join("ready.json")) {
                    if let Ok(value) = serde_json::from_str::<serde_json::Value>(&json) {
                        if let Some(port) = value["port"].as_u64().filter(|p| *p > 0 && *p <= 65535)
                        {
                            engine.port.store(port as u16, Ordering::SeqCst);
                            if let Some(window) = app.get_webview_window("main") {
                                let _ = window.navigate(format!("http://127.0.0.1:{port}/desktop/connect?token={token}&welcome={welcome}").parse().unwrap());
                            }
                            opened = true;
                        }
                    }
                }
                if started.elapsed() > Duration::from_secs(90) {
                    show_message(&app, "打开工作区耗时较长，请通过菜单打开数据目录查看 desktop.log。仍在等待，不会覆盖数据。");
                }
            }
        }
        if attempt < 3 {
            // 已提交数据留在 SQLite；服务重启成功后重新载入页面。
            show_message(&app, "本地服务意外退出，正在重新连接…");
            thread::sleep(Duration::from_secs(3));
        }
    }
    if let Some(window) = app.get_webview_window("main") {
        let _ = window.navigate("tauri://localhost/index.html".parse().unwrap());
    }
    thread::sleep(Duration::from_secs(1));
    show_message(
        &app,
        "本地工作区启动失败。请从菜单打开数据目录检查 desktop.log；已有数据保留，退出后可重试。",
    );
}
fn main() {
    let engine = Arc::new(Engine::default());
    let setup_engine = engine.clone();
    let app = tauri::Builder::default()
        .plugin(tauri_plugin_single_instance::init(|app, _, _| reveal(app)))
        .setup(move |app| {
            let data = std::env::var_os("WORKOS_DATA_DIR")
                .map(PathBuf::from)
                .unwrap_or(app.path().app_data_dir()?);
            fs::create_dir_all(&data)?;
            #[cfg(unix)]
            {
                use std::os::unix::fs::PermissionsExt;
                fs::set_permissions(&data, fs::Permissions::from_mode(0o700))?;
            }
            let resources = if cfg!(debug_assertions) {
                PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("resources")
            } else {
                app.path().resource_dir()?.join("resources")
            };
            let show = MenuItem::with_id(app, "show", "打开 WorkOS", true, None::<&str>)?;
            let folder = MenuItem::with_id(app, "data", "打开数据与日志目录", true, None::<&str>)?;
            let quit =
                MenuItem::with_id(app, "quit-workos", "退出 WorkOS", true, Some("CmdOrCtrl+Q"))?;
            let menu = Menu::default(app.handle())?;
            menu.append(&Submenu::with_items(
                app,
                "工作区",
                true,
                &[&show, &folder, &quit],
            )?)?;
            app.set_menu(menu)?;
            let folder_path = data.clone();
            app.on_menu_event(move |app, event| match event.id.as_ref() {
                "show" => reveal(app),
                "data" => {
                    let _ = Command::new("/usr/bin/open").arg(&folder_path).spawn();
                }
                "quit-workos" => app.exit(0),
                _ => {}
            });
            let tray_menu = Menu::with_items(app, &[&show, &folder, &quit])?;
            TrayIconBuilder::new()
                .icon(app.default_window_icon().unwrap().clone())
                .tooltip("WorkOS")
                .menu(&tray_menu)
                .build(app)?;
            let navigation_engine = setup_engine.clone();
            let window =
                WebviewWindowBuilder::new(app, "main", WebviewUrl::App("index.html".into()))
                    .title(
                        if std::env::var("WORKOS_DESKTOP_EXAMPLE").as_deref() == Ok("true") {
                            "WorkOS · 示例工作区"
                        } else {
                            "WorkOS"
                        },
                    )
                    .inner_size(1280.0, 850.0)
                    .min_inner_size(760.0, 600.0)
                    .on_navigation(move |url| {
                        (url.scheme() == "tauri" && url.host_str() == Some("localhost"))
                            || (url.scheme() == "http"
                                && url.host_str() == Some("127.0.0.1")
                                && url.port()
                                    == Some(navigation_engine.port.load(Ordering::SeqCst)))
                    })
                    .build()?;
            let close_window = window.clone();
            window.on_window_event(move |event| {
                if let tauri::WindowEvent::CloseRequested { api, .. } = event {
                    api.prevent_close();
                    let _ = close_window.hide();
                }
            });
            let handle = app.handle().clone();
            thread::spawn(move || supervise(handle, setup_engine, resources, data));
            Ok(())
        })
        .build(tauri::generate_context!())
        .expect("无法创建 WorkOS 窗口");
    app.run(move |app, event| match event {
        tauri::RunEvent::Exit => stop(&engine),
        #[cfg(target_os = "macos")]
        tauri::RunEvent::Reopen { .. } => reveal(app),
        _ => {}
    });
}
