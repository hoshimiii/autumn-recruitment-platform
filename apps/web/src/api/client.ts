export type JobStatus = "待评估" | "待投递" | "已投递" | "需人工处理";

export type Job = {
  id: string;
  sourceType: string;
  company: string;
  title: string;
  jobType: string;
  city: string;
  techStack: string[];
  certificates: string[];
  deadline: string;
  status: JobStatus;
  description: string;
  requirements: string[];
  snapshots: string[];
  applicationInfo: string;
};

export type ReviewItem = {
  id: string;
  title: string;
  company: string;
  reason: string;
  priority: "高" | "中" | "低";
};

export type Source = {
  id: string;
  name: string;
  type: string;
  status: string;
  lastSync: string;
  needsLogin: boolean;
  needsCaptcha: boolean;
};

export type Task = {
  id: string;
  title: string;
  owner: string;
  due: string;
  state: string;
};

export type WorkbenchData = {
  jobs: Job[];
  reviewQueue: ReviewItem[];
  sources: Source[];
  tasks: Task[];
};

export function getWorkbenchSeed(): WorkbenchData {
  return {
    jobs: [
      {
        id: "job-001",
        sourceType: "校招官网",
        company: "云澜科技",
        title: "前端工程师",
        jobType: "全职",
        city: "上海",
        techStack: ["React", "TypeScript", "Vite"],
        certificates: ["英语六级"],
        deadline: "2026-07-15",
        status: "待投递",
        description: "负责招聘平台候选人端与运营端界面，参与组件库与性能优化。",
        requirements: ["计算机相关专业", "熟悉 React 状态管理", "具备基础工程化经验"],
        snapshots: ["岗位详情页已归档", "薪资范围已截图", "投递入口可用"],
        applicationInfo: "推荐使用中文简历，附件格式 PDF，投递后 3 个工作日内跟进。"
      },
      {
        id: "job-002",
        sourceType: "招聘平台",
        company: "北辰数据",
        title: "数据开发实习生",
        jobType: "实习",
        city: "北京",
        techStack: ["Python", "SQL", "Spark"],
        certificates: ["无硬性要求"],
        deadline: "2026-07-02",
        status: "待评估",
        description: "建设离线数据链路，维护招聘漏斗指标看板。",
        requirements: ["每周到岗 4 天以上", "理解数据仓库分层", "能编写稳定 SQL"],
        snapshots: ["平台列表页已归档", "JD 文本已抽取"],
        applicationInfo: "需要补充项目经历后再投递。"
      },
      {
        id: "job-003",
        sourceType: "企业微信",
        company: "新启智能",
        title: "算法工程师",
        jobType: "全职",
        city: "深圳",
        techStack: ["Python", "PyTorch", "RAG"],
        certificates: ["英语四级", "论文/竞赛加分"],
        deadline: "2026-07-20",
        status: "需人工处理",
        description: "参与简历解析、岗位匹配和推荐排序模型迭代。",
        requirements: ["熟悉机器学习基础", "有模型评测经验", "能阅读英文论文"],
        snapshots: ["内推链接已保存", "验证码页面待处理"],
        applicationInfo: "投递入口需要验证码，处理后同步状态。"
      }
    ],
    reviewQueue: [
      {
        id: "review-001",
        title: "算法工程师",
        company: "新启智能",
        reason: "证书要求与候选人档案匹配度待确认",
        priority: "高"
      },
      {
        id: "review-002",
        title: "数据开发实习生",
        company: "北辰数据",
        reason: "到岗时间需要人工判断",
        priority: "中"
      }
    ],
    sources: [
      {
        id: "source-001",
        name: "云澜校招",
        type: "官网",
        status: "正常",
        lastSync: "2026-06-28 09:10",
        needsLogin: false,
        needsCaptcha: false
      },
      {
        id: "source-002",
        name: "招聘平台 A",
        type: "平台",
        status: "待登录",
        lastSync: "2026-06-27 21:30",
        needsLogin: true,
        needsCaptcha: false
      },
      {
        id: "source-003",
        name: "企业微信内推",
        type: "内推",
        status: "验证码",
        lastSync: "2026-06-28 08:42",
        needsLogin: false,
        needsCaptcha: true
      }
    ],
    tasks: [
      { id: "task-001", title: "补齐前端简历关键词", owner: "运营", due: "今天", state: "进行中" },
      { id: "task-002", title: "复核 2 条高优先级岗位", owner: "审核", due: "明天", state: "待处理" },
      { id: "task-003", title: "导出本周投递列表", owner: "系统", due: "周五", state: "排队中" }
    ]
  };
}
