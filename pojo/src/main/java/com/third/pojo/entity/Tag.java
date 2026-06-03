package com.third.pojo.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 标签
 * </p>
 *
 * @author bsgm
 * @since 2026-04-15
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "Tag对象", description = "标签")
public class Tag implements Serializable {

        private static final long serialVersionUID = 1L;

      /**
     * 标签id
     */
      @ApiModelProperty("标签id")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

      /**
     * 标签名
     */
      @ApiModelProperty("标签名")
    private String name;

    private LocalDateTime gmtCreate;

}
