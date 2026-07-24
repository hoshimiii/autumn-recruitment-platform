const labels: Record<string, string> = {
  SHANDONG_SOE: "山东国企",
  CENTRAL_SOE: "央企",
  INTERNET_BIG_TECH: "互联网大厂",
  SME: "中小厂",
  FOREIGN: "外企",
  OTHER: "其他",
  COMPANY: "企业官网",
  CAMPUS: "校招官网",
  PLATFORM: "招聘平台",
  GOVERNMENT: "政府/国资",
  OFFICIAL: "官网",
  TRUSTED_INDEX: "可信索引",
  LEAD_ONLY: "待核验线索",
  MANUAL: "人工",
  PUBLISHED: "已发布",
  PENDING_REVIEW: "待复核",
  ALLOWED: "已允许",
  UNKNOWN: "待确认",
  REVIEW_REQUIRED: "需核验",
  FORBIDDEN: "禁止采集",
  LOGIN_REQUIRED: "需要登录",
  CAPTCHA_REQUIRED: "需要验证码",
  SUCCESS: "成功",
  BLOCKED: "已阻止",
  FAILED: "失败",
  PARTIAL_FAILURE: "部分失败",
  NOT_MODIFIED: "无变化",
  FULL_TIME: "全职",
  INTERNSHIP: "实习"
};

export function displayLabel(value?: string): string {
  return value ? labels[value] ?? value : "-";
}

export function priorityLabel(value: number): string {
  return value > 0 ? `P${value}` : "-";
}
