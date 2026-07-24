INSERT INTO sources (
  name,type,base_url,list_url,adapter_type,mode,compliance_status,trust_level,scope,
  enabled,max_pages,max_items,selectors_json,field_mapping_json,robots_url
) VALUES (
  'Tencent Careers JSON','COMPANY','https://careers.tencent.com',
  'https://careers.tencent.com/tencentcareer/api/post/Query?pageIndex=1&pageSize=20&language=zh-cn&area=cn&attrId=2,3',
  'JSON','JSON','UNKNOWN','OFFICIAL','INTERNET_BIG_TECH',0,1,20,
  '{"itemsPath":"Data.Posts"}',
  '{"companyName":"$const:Tencent","title":"RecruitPostName","city":"LocationName","jobFamily":"CategoryName","jd":"Responsibility","requirements":"Requirement","jobType":"$const:FULL_TIME","sourceTrust":"$const:OFFICIAL","sourceUrlTemplate":"https://careers.tencent.com/jobdesc.html?postId={PostId}","applyUrl":"$sourceUrl"}',
  'https://careers.tencent.com/robots.txt'
);

INSERT INTO sources (
  name,type,base_url,list_url,adapter_type,mode,compliance_status,trust_level,scope,
  enabled,selectors_json,field_mapping_json,robots_url
) VALUES (
  'State-owned Assets Supervision Recruitment Index','GOVERNMENT','https://www.sasac.gov.cn',
  'https://www.sasac.gov.cn/n2588035/n2588325/n2588350/index.html',
  'HTML','HTML','UNKNOWN','OFFICIAL','CENTRAL_SOE',0,
  '{"item":"a[href]","title":"text","href":"href","detail":"text"}',
  '{"companyName":"$const:State-owned Assets Supervision Recruitment Index","jobType":"$const:FULL_TIME","sourceTrust":"$const:OFFICIAL"}',
  'https://www.sasac.gov.cn/robots.txt'
);

INSERT INTO sources (
  name,type,base_url,list_url,adapter_type,mode,compliance_status,trust_level,scope,
  enabled,selectors_json,field_mapping_json,robots_url
) VALUES (
  'Shandong Rural Credit Recruitment','COMPANY','https://www.sdrcu.com',
  'https://www.sdrcu.com/sdnxs/374215/374221/index.html',
  'HTML','HTML','UNKNOWN','OFFICIAL','SHANDONG_SOE',0,
  '{"item":"a[href]","title":"text","href":"href","date":".date","detail":"text"}',
  '{"companyName":"$const:Shandong Rural Credit","jobType":"$const:FULL_TIME","sourceTrust":"$const:OFFICIAL"}',
  'https://www.sdrcu.com/robots.txt'
);

INSERT INTO sources (
  name,type,base_url,list_url,adapter_type,mode,compliance_status,trust_level,scope,enabled
) VALUES (
  'Alibaba Campus Recruitment','COMPANY','https://campus-talent.alibaba.com',
  'https://campus-talent.alibaba.com/','MANUAL','MANUAL','UNKNOWN','OFFICIAL',
  'INTERNET_BIG_TECH',0
);

INSERT INTO sources (
  name,type,base_url,list_url,adapter_type,mode,compliance_status,trust_level,scope,enabled
) VALUES (
  'Huawei Campus Recruitment','COMPANY','https://career.huawei.com',
  'https://career.huawei.com/cn/campus-recruitment','MANUAL','MANUAL','UNKNOWN','OFFICIAL',
  'INTERNET_BIG_TECH',0
);
