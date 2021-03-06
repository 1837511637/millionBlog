package com.kcy.common.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@ApiModel(value = "Page", description = "分页")
@Data
public class PageConst implements Serializable {
    @ApiModelProperty(value = "第几页")
    public static int page = 1;
    @ApiModelProperty(value = "条数")
    public static int rows = 10;
}
