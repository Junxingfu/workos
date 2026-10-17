package com.workos.common.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.util.CollectionUtils;

/** 分页对象 */
@Getter
@Setter
@NoArgsConstructor
@ToString
@Accessors(chain = true)
public class BasePageResult<T> implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  /** 总数 */
  private Long total;

  /** 当前页 */
  private Long pageNo;

  /** 每页记录数 */
  private Long pageSize;

  /** 数据 */
  private List<T> dataList;

  public BasePageResult(List<T> dataList, Long pageNo, Long pageSize, Long total) {
    this.dataList = CollectionUtils.isEmpty(dataList) ? Collections.emptyList() : dataList;
    this.pageNo = pageNo;
    this.pageSize = pageSize;
    this.total = total;
  }
}
