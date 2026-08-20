-- 题目标程（仅 C++，一般不超过数百行，TEXT 足够）
ALTER TABLE problem
  ADD COLUMN standard_code TEXT NULL COMMENT 'C++标程源码' AFTER is_public;
