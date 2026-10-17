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
    token: Mutex<String>,
    widget_preferences: Mutex<PathBuf>,
}
fn reveal(app: &tauri::AppHandle) {
    if let Some(window) = app.get_webview_window("main") {
        let _ = window.show();
        let _ = window.unminimize();
        let _ = window.set_focus();
    }
}
// 浮窗偏好独立于后端随机端口，在工作区保存，重启后仍可恢复。
fn widget_preferences(engine: &Engine) -> serde_json::Value {
    fs::read_to_string(engine.widget_preferences.lock().unwrap().as_path())
        .ok()
        .and_then(|text| serde_json::from_str(&text).ok())
        .unwrap_or_else(|| serde_json::json!({"opacity": 92, "width": 380, "height": 520}))
}
fn save_widget_preferences(engine: &Engine, preferences: &serde_json::Value) {
    let path = engine.widget_preferences.lock().unwrap();
    let temporary = path.with_extension("json.tmp");
    if fs::write(&temporary, preferences.to_string()).is_ok() {
        if let Err(error) = fs::rename(&temporary, path.as_path()) {
            eprintln!("无法保存浮窗偏好：{error}");
        }
    }
}
fn show_focus(app: &tauri::AppHandle, engine: Arc<Engine>) {
    let port = engine.port.load(Ordering::SeqCst);
    if port == 0 {
        return;
    }
    if let Some(window) = app.get_webview_window("focus") {
        let _ = window.show();
        let _ = window.set_focus();
        return;
    }
    let token = engine.token.lock().unwrap().clone();
    let url = format!("http://127.0.0.1:{port}/desktop/connect?token={token}&view=focus");
    let preferences = widget_preferences(&engine);
    let opacity = preferences["opacity"].as_u64().unwrap_or(92).clamp(35, 100);
    let initialization =
        format!("window.__WORKOS_WIDGET__ = true; window.__WORKOS_WIDGET_OPACITY__ = {opacity};");
    let nav_app = app.clone();
    let event_engine = engine.clone();
    let result = WebviewWindowBuilder::new(app, "focus", WebviewUrl::External(url.parse().unwrap()))
        .initialization_script(&initialization)
        .title("WorkOS · 待办")
        .inner_size(280.0, 76.0)
        .decorations(false)
        .resizable(false)
        .transparent(true)
        .shadow(false)
        .skip_taskbar(true)
        .visible_on_all_workspaces(true)
        .always_on_top(true)
        .on_navigation(move |url| {
            let local = url.scheme() == "http" && url.host_str() == Some("127.0.0.1")
                && url.port() == Some(engine.port.load(Ordering::SeqCst));
            if local && url.path() == "/desktop/window/main" {
                reveal(&nav_app);
                if let Some((_, id)) = url.query_pairs().find(|(key, _)| key == "item") {
                    if !id.is_empty() && id.len() <= 19 && id.chars().all(|ch| ch.is_ascii_digit()) {
                        if let Some(window) = nav_app.get_webview_window("main") {
                            // 同源路由事件保护主窗口未保存的输入，不强制刷新页面。
                            let script = format!("window.dispatchEvent(new CustomEvent('workos:open-item', {{ detail: '{id}' }}));");
                            let _ = window.eval(&script);
                        }
                    }
                }
                return false;
            }
            if local {
                if let Some(action) = url.path().strip_prefix("/desktop/window/focus/") {
                    let action = action.to_owned();
                    let opacity = url.query_pairs().find(|(key, _)| key == "value")
                        .and_then(|(_, value)| value.parse::<u64>().ok()).map(|value| value.clamp(35, 100));
                    let dimensions: Vec<f64> = ["width", "height"].iter().map(|name|
                        url.query_pairs().find(|(key, _)| key == name)
                            .and_then(|(_, value)| value.parse::<f64>().ok()).unwrap_or(0.0)).collect();
                    let preferences_engine = engine.clone();
                    let handle = nav_app.clone();
                    let _ = nav_app.run_on_main_thread(move || {
                        if let Some(window) = handle.get_webview_window("focus") {
                            match action.as_str() {
                                "expand" => {
                                    let preferences = widget_preferences(&preferences_engine);
                                    let width = preferences["width"].as_f64().unwrap_or(380.0).clamp(300.0, 900.0);
                                    let height = preferences["height"].as_f64().unwrap_or(520.0).clamp(300.0, 1000.0);
                                    let _ = window.set_resizable(true);
                                    let _ = window.set_min_size(Some(tauri::LogicalSize::new(300.0, 300.0)));
                                    let _ = window.set_max_size(Some(tauri::LogicalSize::new(900.0, 1000.0)));
                                    resize_focus(&window, width, height);
                                }
                                "collapse" => {
                                    let mut preferences = widget_preferences(&preferences_engine);
                                    if let (Ok(size), Ok(scale)) = (window.inner_size(), window.scale_factor()) {
                                        preferences["width"] = serde_json::json!(size.width as f64 / scale);
                                        preferences["height"] = serde_json::json!(size.height as f64 / scale);
                                    }
                                    save_widget_preferences(&preferences_engine, &preferences);
                                    let _ = window.set_min_size(None::<tauri::LogicalSize<f64>>);
                                    let _ = window.set_max_size(None::<tauri::LogicalSize<f64>>);
                                    let _ = window.set_resizable(false);
                                    resize_focus(&window, 280.0, 76.0);
                                }
                                "opacity" => {
                                    if let Some(value) = opacity {
                                        let mut preferences = widget_preferences(&preferences_engine);
                                        preferences["opacity"] = serde_json::json!(value);
                                        save_widget_preferences(&preferences_engine, &preferences);
                                    }
                                }
                                "resize" => {
                                    // macOS 不支持 start_resize_dragging；用捕获指针的屏幕位移调整。
                                    if dimensions.iter().all(|value| value.is_finite() && *value > 0.0) {
                                        resize_focus(&window, dimensions[0].clamp(300.0, 900.0), dimensions[1].clamp(300.0, 1000.0));
                                    }
                                }
                                "remember" => {
                                    if let (Ok(size), Ok(scale)) = (window.inner_size(), window.scale_factor()) {
                                        let mut preferences = widget_preferences(&preferences_engine);
                                        preferences["width"] = serde_json::json!(size.width as f64 / scale);
                                        preferences["height"] = serde_json::json!(size.height as f64 / scale);
                                        save_widget_preferences(&preferences_engine, &preferences);
                                    }
                                }
                                "hide" => { let _ = window.hide(); }
                                "drag" => { let _ = window.start_dragging(); }
                                _ => {}
                            }
                        }
                    });
                    return false;
                }
            }
            local
        })
        .build();
    match result {
        Ok(window) => {
            let close_window = window.clone();
            window.on_window_event(move |event| {
                if let tauri::WindowEvent::Resized(size) = event {
                    if let Ok(scale) = close_window.scale_factor() {
                        let width = size.width as f64 / scale;
                        let height = size.height as f64 / scale;
                        if width >= 300.0 && height >= 300.0 {
                            let mut preferences = widget_preferences(&event_engine);
                            preferences["width"] = serde_json::json!(width);
                            preferences["height"] = serde_json::json!(height);
                            save_widget_preferences(&event_engine, &preferences);
                        }
                    }
                }
                if let tauri::WindowEvent::CloseRequested { api, .. } = event {
                    api.prevent_close();
                    let _ = close_window.hide();
                }
            });
        }
        Err(error) => eprintln!("无法打开任务浮窗：{error}"),
    }
}
// 展开时保留浮窗位置，靠近屏幕边缘则向内移动，避免简要清单落到屏幕外。
fn resize_focus(window: &tauri::WebviewWindow, width: f64, height: f64) {
    let _ = window.set_size(tauri::LogicalSize::new(width, height));
    if let (Ok(Some(monitor)), Ok(position)) = (window.current_monitor(), window.outer_position()) {
        let scale = monitor.scale_factor();
        let area = monitor.work_area();
        let origin = &area.position;
        let size = &area.size;
        let max_x = origin.x + size.width as i32 - (width * scale).ceil() as i32;
        let max_y = origin.y + size.height as i32 - (height * scale).ceil() as i32;
        let x = position.x.clamp(origin.x, max_x.max(origin.x));
        let y = position.y.clamp(origin.y, max_y.max(origin.y));
        let _ = window.set_position(tauri::PhysicalPosition::new(x, y));
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
                            *engine.token.lock().unwrap() = token.clone();
                            if let Some(window) = app.get_webview_window("focus") {
                                let _ = window.set_min_size(None::<tauri::LogicalSize<f64>>);
                                let _ = window.set_max_size(None::<tauri::LogicalSize<f64>>);
                                let _ = window.set_resizable(false);
                                resize_focus(&window, 280.0, 76.0);
                                let _ = window.navigate(format!("http://127.0.0.1:{port}/desktop/connect?token={token}&view=focus").parse().unwrap());
                            }
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
            *setup_engine.widget_preferences.lock().unwrap() = data.join("widget-preferences.json");
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
            let focus = MenuItem::with_id(app, "focus", "打开任务浮窗", true, None::<&str>)?;
            let folder = MenuItem::with_id(app, "data", "打开数据与日志目录", true, None::<&str>)?;
            let quit =
                MenuItem::with_id(app, "quit-workos", "退出 WorkOS", true, Some("CmdOrCtrl+Q"))?;
            let menu = Menu::default(app.handle())?;
            menu.append(&Submenu::with_items(
                app,
                "工作区",
                true,
                &[&show, &focus, &folder, &quit],
            )?)?;
            app.set_menu(menu)?;
            let folder_path = data.clone();
            let menu_engine = setup_engine.clone();
            app.on_menu_event(move |app, event| match event.id.as_ref() {
                "show" => reveal(app),
                "focus" => show_focus(app, menu_engine.clone()),
                "data" => {
                    let _ = Command::new("/usr/bin/open").arg(&folder_path).spawn();
                }
                "quit-workos" => app.exit(0),
                _ => {}
            });
            let tray_menu = Menu::with_items(app, &[&show, &focus, &folder, &quit])?;
            TrayIconBuilder::new()
                .icon(app.default_window_icon().unwrap().clone())
                .tooltip("WorkOS")
                .menu(&tray_menu)
                .build(app)?;
            let navigation_engine = setup_engine.clone();
            let navigation_app = app.handle().clone();
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
                        if url.scheme() == "http"
                            && url.host_str() == Some("127.0.0.1")
                            && url.port() == Some(navigation_engine.port.load(Ordering::SeqCst))
                            && url.path() == "/desktop/window/focus"
                        {
                            let handle = navigation_app.clone();
                            let engine = navigation_engine.clone();
                            let _ = navigation_app
                                .run_on_main_thread(move || show_focus(&handle, engine));
                            return false;
                        }
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
