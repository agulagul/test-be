package com.skripsi.koma.dto.billing;

import com.skripsi.koma.enums.ApprovalStatus;

import lombok.Data;

@Data
public class BillingApprovalDTO {
  private ApprovalStatus decision;
  private String remarks;
}
