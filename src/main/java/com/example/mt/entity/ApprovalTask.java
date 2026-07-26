package com.example.mt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("approval_task")
public class ApprovalTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String title;
    private String applicant;
    private String status;      // PENDING / APPROVED / REJECTED
    private String nodeTypes;    // 逗号分隔：LEADER,HR,FINANCE
    private LocalDateTime createTime;
}
