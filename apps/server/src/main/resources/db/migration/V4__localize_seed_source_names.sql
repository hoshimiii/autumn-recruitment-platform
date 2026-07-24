UPDATE sources SET name='腾讯校招官网',
  field_mapping_json=replace(field_mapping_json, '$const:Tencent', '$const:腾讯')
WHERE name='Tencent Careers JSON' AND base_url='https://careers.tencent.com';

UPDATE sources SET name='国资委招聘信息',
  field_mapping_json=replace(
    field_mapping_json,
    '$const:State-owned Assets Supervision Recruitment Index',
    '$const:国资委招聘信息')
WHERE name='State-owned Assets Supervision Recruitment Index'
  AND base_url='https://www.sasac.gov.cn';

UPDATE sources SET name='山东农信招聘',
  field_mapping_json=replace(
    field_mapping_json,
    '$const:Shandong Rural Credit',
    '$const:山东农信')
WHERE name='Shandong Rural Credit Recruitment'
  AND base_url='https://www.sdrcu.com';

UPDATE sources SET name='阿里巴巴校园招聘'
WHERE name='Alibaba Campus Recruitment'
  AND base_url='https://campus-talent.alibaba.com';

UPDATE sources SET name='华为校园招聘'
WHERE name='Huawei Campus Recruitment'
  AND base_url='https://career.huawei.com';
