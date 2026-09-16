-- ============================================
-- LikeU 词根背单词 - 演示数据初始化SQL
-- 1本词书 + 50个常见词根 + 60个单词 + 关联关系
-- ============================================

-- ========== 1. 词书 ==========
INSERT INTO `t_word_book` (`id`, `name`, `description`, `word_count`, `sort_order`)
VALUES (1, 'CET-4 高频词根词缀', '精选含有常见词根词缀的CET-4核心词汇，边学单词边记词根', 60, 1);

-- ========== 2. 词根词缀 ==========
INSERT INTO `t_root` (`id`, `type`, `root`, `meaning`, `origin`, `example`) VALUES
(1, 2, 'dict', '说，断言', '拉丁语', 'predict, dictate, dictionary, contradict'),
(2, 2, 'port', '搬运，携带', '拉丁语', 'export, import, transport, portable'),
(3, 2, 'ject', '投掷，扔', '拉丁语', 'project, inject, reject, subject'),
(4, 1, 'pre', '在...之前，预先', '拉丁语', 'preview, predict, prepay, prepare'),
(5, 1, 'un', '不，非', '古英语', 'unable, unhappy, uncommon, unknown'),
(6, 1, 'dis', '不，否定，分开', '拉丁语', 'disable, disagree, discover, disconnect'),
(7, 1, 're', '再，又，重新', '拉丁语', 'review, return, replay, rewrite'),
(8, 2, 'aud', '听', '拉丁语', 'audio, audience, auditorium'),
(9, 2, 'vis', '看', '拉丁语', 'visible, vision, visit, television'),
(10, 2, 'scribe/script', '写，记录', '拉丁语', 'describe, script, subscribe, postscript'),
(11, 2, 'act', '做，行动，驱动', '拉丁语', 'action, active, react, interact'),
(12, 2, 'struct', '建造，构建', '拉丁语', 'structure, construct, destroy, instruct'),
(13, 2, 'spect', '看，观察', '拉丁语', 'inspect, respect, suspect, prospect'),
(14, 2, 'tract', '拉，拖，引', '拉丁语', 'attract, extract, contract, subtract'),
(15, 1, 'bene', '好，善', '拉丁语', 'benefit, beneficial, benevolent'),
(16, 1, 'auto', '自己，自身', '希腊语', 'automatic, automobile, autograph'),
(17, 2, 'bio', '生命，生物', '希腊语', 'biology, biography, antibiotic'),
(18, 2, 'geo', '地球，土地', '希腊语', 'geography, geology, geometry'),
(19, 1, 'tele', '远距离', '希腊语', 'telephone, television, telegraph, telescope'),
(20, 3, '-tion/-sion', '名词后缀，表行为/状态', '拉丁语', 'action, education, decision, information'),
(21, 3, '-able', '能...的，可...的', '拉丁语', 'portable, readable, comfortable, predictable'),
(22, 3, '-ful', '充满...的', '古英语', 'beautiful, helpful, useful, wonderful'),
(23, 3, '-less', '没有...的，不...的', '古英语', 'careless, homeless, helpless, endless'),
(24, 3, '-ly', '副词后缀，...地', '古英语', 'quickly, carefully, happily, easily'),
(25, 3, '-ment', '名词后缀，表行为/状态', '拉丁语', 'development, movement, achievement, agreement'),
(26, 3, '-ness', '名词后缀，表性质/状态', '古英语', 'happiness, kindness, darkness, business'),
(27, 1, 'inter', '在...之间，互相', '拉丁语', 'international, interact, interview, internet'),
(28, 1, 'trans', '横跨，转移，变化', '拉丁语', 'transport, translate, transform, transplant'),
(29, 1, 'anti', '反对，抗', '希腊语', 'antibiotic, antisocial, antiwar, antifungal'),
(30, 1, 'ex', '向外，出', '拉丁语', 'export, exit, extract, exclude'),
(31, 1, 'sub', '在下面，次，亚', '拉丁语', 'subway, submarine, subtract, submit'),
(32, 1, 'pro', '向前，赞成', '拉丁语', 'project, product, promote, propose'),
(33, 1, 'de', '向下，去除，否定', '拉丁语', 'decrease, destroy, depart, delete'),
(34, 1, 'con/com', '共同，一起', '拉丁语', 'connect, combine, contain, collect'),
(35, 1, 'mis', '错误，坏', '古英语', 'mistake, misunderstand, mislead, misplace'),
(36, 1, 'multi', '多', '拉丁语', 'multiple, multimedia, multinational, multiply'),
(37, 1, 'bi', '二，双', '拉丁语', 'bicycle, bilingual, biweekly, biannual'),
(38, 1, 'tri', '三', '希腊语', 'triangle, tricycle, triple, trio'),
(39, 1, 'uni', '一，单一', '拉丁语', 'unify, unit, uniform, universe'),
(40, 2, 'centr', '中心', '拉丁语', 'center, central, concentrate, eccentric'),
(41, 2, 'duc/duct', '引导，带领', '拉丁语', 'introduce, produce, reduce, conduct'),
(42, 2, 'form', '形状，形式', '拉丁语', 'inform, reform, transform, uniform'),
(43, 2, 'press', '压，挤', '拉丁语', 'express, impress, depress, compress'),
(44, 2, 'vert/vers', '转，转变', '拉丁语', 'convert, reverse, diverse, version'),
(45, 2, 'mov/mot', '移动，运动', '拉丁语', 'move, motion, promote, remote'),
(46, 2, 'pend', '悬挂，支付', '拉丁语', 'depend, independent, suspend, pendant'),
(47, 2, 'sens/sent', '感觉，感知', '拉丁语', 'sense, sensation, sensitive, sentiment'),
(48, 2, 'sign', '标记，符号', '拉丁语', 'signal, design, assign, resign'),
(49, 2, 'solv/solu', '解开，解决', '拉丁语', 'solve, solution, resolve, dissolve'),
(50, 2, 'tend/tens', '伸展，趋向', '拉丁语', 'extend, intention, attention, tension');

-- ========== 3. 单词数据 ==========
-- 词书ID=1, sort_order按顺序递增
INSERT INTO `t_word` (`id`, `word_book_id`, `word`, `phonetic_uk`, `phonetic_us`, `meaning_cn`, `example_en`, `example_cn`, `sort_order`) VALUES

-- 词根 dict 相关
(1, 1, 'predict', '/prɪˈdɪkt/', '/prɪˈdɪkt/', '预测，预言', 'Scientists predict that the weather will get warmer.', '科学家预测天气会变得更暖和。', 1),
(2, 1, 'dictate', '/dɪkˈteɪt/', '/ˈdɪkteɪt/', '口述，命令，支配', 'The boss dictated a letter to his secretary.', '老板向秘书口述了一封信。', 2),
(3, 1, 'dictionary', '/ˈdɪkʃənri/', '/ˈdɪkʃəneri/', '词典，字典', 'Look up the word in the dictionary.', '在词典里查一下这个词。', 3),
(4, 1, 'contradict', '/ˌkɒntrəˈdɪkt/', '/ˌkɑːntrəˈdɪkt/', '反驳，与...矛盾', 'His actions contradict his words.', '他的行为与他的话相矛盾。', 4),

-- 词根 port 相关
(5, 1, 'export', '/ɪkˈspɔːt/', '/ɪkˈspɔːrt/', '出口，输出', 'China exports many products to the world.', '中国向世界出口许多产品。', 5),
(6, 1, 'import', '/ɪmˈpɔːt/', '/ɪmˈpɔːrt/', '进口，输入', 'The country imports oil from abroad.', '这个国家从国外进口石油。', 6),
(7, 1, 'transport', '/ˈtrænspɔːt/', '/ˈtrænspɔːrt/', '运输，交通', 'The goods were transported by train.', '货物通过火车运输。', 7),
(8, 1, 'portable', '/ˈpɔːtəbl/', '/ˈpɔːrtəbl/', '便携的，轻便的', 'I bought a portable charger for my phone.', '我买了一个便携式手机充电器。', 8),

-- 词根 ject 相关
(9, 1, 'project', '/ˈprɒdʒekt/', '/ˈprɑːdʒekt/', '项目，计划；投影', 'We are working on a new project.', '我们正在做一个新项目。', 9),
(10, 1, 'inject', '/ɪnˈdʒekt/', '/ɪnˈdʒekt/', '注射，注入', 'The doctor injected the vaccine into his arm.', '医生把疫苗注射进他的手臂。', 10),
(11, 1, 'reject', '/rɪˈdʒekt/', '/rɪˈdʒekt/', '拒绝，拒收', 'She rejected his invitation.', '她拒绝了他的邀请。', 11),
(12, 1, 'subject', '/ˈsʌbdʒɪkt/', '/ˈsʌbdʒɪkt/', '主题，科目；使服从', 'History is my favorite subject.', '历史是我最喜欢的科目。', 12),

-- 词根 spect 相关
(13, 1, 'inspect', '/ɪnˈspekt/', '/ɪnˈspekt/', '检查，审视', 'The inspector inspected the factory.', '检查员检查了工厂。', 13),
(14, 1, 'respect', '/rɪˈspekt/', '/rɪˈspekt/', '尊重，尊敬', 'We should respect our teachers.', '我们应该尊重老师。', 14),
(15, 1, 'suspect', '/səˈspekt/', '/səˈspekt/', '怀疑，嫌疑犯', 'The police suspect him of stealing.', '警方怀疑他偷窃。', 15),
(16, 1, 'prospect', '/ˈprɒspekt/', '/ˈprɑːspekt/', '前景，展望', 'There is little prospect of success.', '成功的希望渺茫。', 16),

-- 词根 struct 相关
(17, 1, 'structure', '/ˈstrʌktʃər/', '/ˈstrʌktʃər/', '结构，建筑物', 'The building has a modern structure.', '这栋建筑有着现代化的结构。', 17),
(18, 1, 'construct', '/kənˈstrʌkt/', '/kənˈstrʌkt/', '建造，构造', 'They constructed a new bridge.', '他们建造了一座新桥。', 18),
(19, 1, 'destroy', '/dɪˈstrɔɪ/', '/dɪˈstrɔɪ/', '破坏，毁灭', 'The earthquake destroyed the city.', '地震摧毁了这座城市。', 19),
(20, 1, 'instruct', '/ɪnˈstrʌkt/', '/ɪnˈstrʌkt/', '指导，指示', 'The teacher instructed the students.', '老师指导学生。', 20),

-- 词根 tract 相关
(21, 1, 'attract', '/əˈtrækt/', '/əˈtrækt/', '吸引，引起', 'The museum attracts many tourists.', '博物馆吸引了很多游客。', 21),
(22, 1, 'extract', '/ɪkˈstrækt/', '/ɪkˈstrækt/', '提取，摘录', 'The dentist extracted my tooth.', '牙医拔掉了我的牙。', 22),
(23, 1, 'contract', '/ˈkɒntrækt/', '/ˈkɑːntrækt/', '合同，契约', 'He signed a contract with the company.', '他和公司签了一份合同。', 23),
(24, 1, 'subtract', '/səbˈtrækt/', '/səbˈtrækt/', '减去，扣除', 'Subtract 5 from 10, you get 5.', '10减去5等于5。', 24),

-- 词根 vis/aud 相关
(25, 1, 'visible', '/ˈvɪzəbl/', '/ˈvɪzəbl/', '可见的，明显的', 'The stars are visible tonight.', '今晚看得见星星。', 25),
(26, 1, 'vision', '/ˈvɪʒn/', '/ˈvɪʒn/', '视力，视野，愿景', 'He has poor vision.', '他视力不好。', 26),
(27, 1, 'audio', '/ˈɔːdiəʊ/', '/ˈɔːdioʊ/', '音频的，声音的', 'The audio quality is excellent.', '音频质量非常好。', 27),
(28, 1, 'audience', '/ˈɔːdiəns/', '/ˈɔːdiəns/', '观众，听众', 'The audience applauded loudly.', '观众热烈鼓掌。', 28),

-- 前缀 un- 相关
(29, 1, 'unable', '/ʌnˈeɪbl/', '/ʌnˈeɪbl/', '不能的，无法的', 'I am unable to attend the meeting.', '我无法参加会议。', 29),
(30, 1, 'unhappy', '/ʌnˈhæpi/', '/ʌnˈhæpi/', '不开心的，不快乐的', 'She looks unhappy today.', '她今天看起来不开心。', 30),
(31, 1, 'uncommon', '/ʌnˈkɒmən/', '/ʌnˈkɑːmən/', '不常见的，罕见的', 'This bird is uncommon in this area.', '这种鸟在这个地区不常见。', 31),
(32, 1, 'unknown', '/ˌʌnˈnəʊn/', '/ˌʌnˈnoʊn/', '未知的，不知名的', 'The cause of the accident is unknown.', '事故原因未知。', 32),

-- 前缀 re- 相关
(33, 1, 'review', '/rɪˈvjuː/', '/rɪˈvjuː/', '复习，回顾，评论', 'Let''s review what we learned today.', '让我们复习今天学的内容。', 33),
(34, 1, 'return', '/rɪˈtɜːn/', '/rɪˈtɜːrn/', '返回，归还', 'She returned home late.', '她很晚才回家。', 34),
(35, 1, 'replay', '/ˌriːˈpleɪ/', '/ˌriːˈpleɪ/', '重放，重播', 'They replayed the goal on TV.', '电视上重播了那个进球。', 35),

-- 前缀 dis- 相关
(36, 1, 'disable', '/dɪsˈeɪbl/', '/dɪsˈeɪbl/', '使无效，使残疾', 'The virus disabled the computer system.', '病毒使电脑系统瘫痪了。', 36),
(37, 1, 'disagree', '/ˌdɪsəˈɡriː/', '/ˌdɪsəˈɡriː/', '不同意，有分歧', 'I disagree with your opinion.', '我不同意你的观点。', 37),
(38, 1, 'discover', '/dɪˈskʌvər/', '/dɪˈskʌvər/', '发现，发觉', 'Columbus discovered America in 1492.', '哥伦布在1492年发现了美洲。', 38),

-- 词根 duc/duct 相关
(39, 1, 'introduce', '/ˌɪntrəˈdjuːs/', '/ˌɪntrəˈduːs/', '介绍，引入', 'Let me introduce my friend to you.', '让我把我的朋友介绍给你。', 39),
(40, 1, 'produce', '/prəˈdjuːs/', '/prəˈduːs/', '生产，产生', 'This factory produces cars.', '这家工厂生产汽车。', 40),
(41, 1, 'reduce', '/rɪˈdjuːs/', '/rɪˈduːs/', '减少，降低', 'We need to reduce costs.', '我们需要降低成本。', 41),
(42, 1, 'conduct', '/kənˈdʌkt/', '/kənˈdʌkt/', '引导，指挥，行为', 'The experiment was conducted by a team.', '这个实验由一个团队进行。', 42),

-- 词根 form 相关
(43, 1, 'inform', '/ɪnˈfɔːm/', '/ɪnˈfɔːrm/', '通知，告知', 'Please inform us of any changes.', '如有任何变化请通知我们。', 43),
(44, 1, 'reform', '/rɪˈfɔːm/', '/rɪˈfɔːrm/', '改革，改进', 'The government is reforming the education system.', '政府正在改革教育体系。', 44),
(45, 1, 'transform', '/trænsˈfɔːm/', '/trænsˈfɔːrm/', '转变，改造', 'The city has transformed in recent years.', '这座城市近年来发生了转变。', 45),
(46, 1, 'uniform', '/ˈjuːnɪfɔːm/', '/ˈjuːnɪfɔːrm/', '制服；统一的', 'Students wear school uniforms.', '学生们穿校服。', 46),

-- 词根 mov/mot 相关
(47, 1, 'move', '/muːv/', '/muːv/', '移动，搬动', 'Please move your chair closer.', '请把你的椅子挪近些。', 47),
(48, 1, 'motion', '/ˈməʊʃn/', '/ˈmoʊʃn/', '运动，动作', 'The motion of the waves is calming.', '波浪的运动让人平静。', 48),
(49, 1, 'promote', '/prəˈməʊt/', '/prəˈmoʊt/', '促进，提升', 'She was promoted to manager.', '她被提升为经理。', 49),
(50, 1, 'remote', '/rɪˈməʊt/', '/rɪˈmoʊt/', '遥远的，偏僻的', 'He lives in a remote village.', '他住在一个偏远的村庄。', 50),

-- 词根 pend 相关
(51, 1, 'depend', '/dɪˈpend/', '/dɪˈpend/', '依赖，取决于', 'It depends on the weather.', '这取决于天气。', 51),
(52, 1, 'independent', '/ˌɪndɪˈpendənt/', '/ˌɪndɪˈpendənt/', '独立的，自主的', 'She is very independent.', '她非常独立。', 52),
(53, 1, 'suspend', '/səˈspend/', '/səˈspend/', '暂停，悬挂', 'The game was suspended due to rain.', '比赛因雨暂停。', 53),

-- 词根 sens/sign 相关
(54, 1, 'sense', '/sens/', '/sens/', '感觉，感官，意义', 'I have a sense of achievement.', '我有一种成就感。', 54),
(55, 1, 'sensitive', '/ˈsensətɪv/', '/ˈsensətɪv/', '敏感的，灵敏的', 'She is sensitive to criticism.', '她对批评很敏感。', 55),
(56, 1, 'signal', '/ˈsɪɡnəl/', '/ˈsɪɡnəl/', '信号，标志', 'He gave a signal to start.', '他发出了开始的信号。', 56),
(57, 1, 'design', '/dɪˈzaɪn/', '/dɪˈzaɪn/', '设计，图案', 'She designed a beautiful dress.', '她设计了一条漂亮的裙子。', 57),

-- 词根 solv/ tend 相关
(58, 1, 'solve', '/sɒlv/', '/sɑːlv/', '解决，解答', 'We need to solve this problem.', '我们需要解决这个问题。', 58),
(59, 1, 'extend', '/ɪkˈstend/', '/ɪkˈstend/', '延伸，扩展', 'We decided to extend our stay.', '我们决定延长停留时间。', 59),
(60, 1, 'attention', '/əˈtenʃn/', '/əˈtenʃn/', '注意力，关注', 'Please pay attention to the details.', '请注意细节。', 60);

-- ========== 4. 单词-词根关联 ==========
-- 关联表的插入顺序: (word_id, root_id, position)
-- dict (root_id=1)
INSERT INTO `t_word_root` (`word_id`, `root_id`, `position`) VALUES
(1, 4, 1), (1, 1, 2),   -- predict = pre(前) + dict(说)
(2, 1, 1),               -- dictate = dict(说) + ate(动词后缀)
(3, 1, 1),               -- dictionary = dict(说) + ion(名词) + ary(名词)
(4, 1, 1),               -- contradict = contra(反对) + dict(说) (contra-暂未收录)

-- port (root_id=2)
(5, 30, 1), (5, 2, 2),  -- export = ex(出) + port(搬运)
(6, 2, 1),               -- import = im(入) + port(搬运)
(7, 28, 1), (7, 2, 2),  -- transport = trans(横跨) + port(搬运)
(8, 2, 1), (8, 21, 2),  -- portable = port(搬运) + able(能...的)

-- ject (root_id=3)
(9, 32, 1), (9, 3, 2),  -- project = pro(向前) + ject(投掷)
(10, 3, 1),              -- inject = in(入) + ject(投掷)
(11, 7, 1), (11, 3, 2), -- reject = re(回) + ject(投掷)
(12, 31, 1), (12, 3, 2),-- subject = sub(在下) + ject(投掷)

-- spect (root_id=13)
(13, 13, 1),             -- inspect = in(向内) + spect(看)
(14, 7, 1), (14, 13, 2),-- respect = re(再) + spect(看)
(15, 31, 1), (15, 13, 2),-- suspect = sus(下) + spect(看)
(16, 32, 1), (16, 13, 2),-- prospect = pro(向前) + spect(看)

-- struct (root_id=12)
(17, 12, 1),             -- structure = struct(建造) + ure(名词)
(18, 34, 1), (18, 12, 2),-- construct = con(共同) + struct(建造)
(19, 33, 1), (19, 12, 2),-- destroy = de(去除) + stroy(建造)
(20, 12, 1),             -- instruct = in(在...) + struct(建造)

-- tract (root_id=14)
(21, 14, 1),             -- attract = at(向) + tract(拉)
(22, 30, 1), (22, 14, 2),-- extract = ex(出) + tract(拉)
(23, 34, 1), (23, 14, 2),-- contract = con(共同) + tract(拉)
(24, 31, 1), (24, 14, 2),-- subtract = sub(下) + tract(拉)

-- vis (root_id=9)
(25, 9, 1), (25, 21, 2),-- visible = vis(看) + ible(能)
(26, 9, 1),              -- vision = vis(看) + ion(名词)

-- aud (root_id=8)
(27, 8, 1),              -- audio = aud(听) + io(名词)
(28, 8, 1),              -- audience = aud(听) + ience(名词)

-- un (root_id=5)
(29, 5, 1),              -- un(不) + able
(30, 5, 1),              -- un(不) + happy
(31, 5, 1),              -- un(不) + common
(32, 5, 1),              -- un(不) + known

-- re (root_id=7)
(33, 7, 1),              -- re(再) + view(看)
(34, 7, 1),              -- re(回) + turn(转)
(35, 7, 1),              -- re(再) + play(播放)

-- dis (root_id=6)
(36, 6, 1),              -- dis(不) + able
(37, 6, 1),              -- dis(不) + agree
(38, 6, 1),              -- dis(不) + cover(覆盖)

-- duc/duct (root_id=41)
(39, 27, 1), (39, 41, 2),-- introduce = intro(向内) + duc(引导)
(40, 32, 1), (40, 41, 2),-- produce = pro(向前) + duc(引导)
(41, 7, 1), (41, 41, 2), -- reduce = re(回) + duc(引导)
(42, 34, 1), (42, 41, 2),-- conduct = con(共同) + duct(引导)

-- form (root_id=42)
(43, 42, 1),             -- inform = in(使) + form(形状)
(44, 7, 1), (44, 42, 2),-- reform = re(再) + form(形状)
(45, 28, 1), (45, 42, 2),-- transform = trans(跨越) + form(形状)
(46, 39, 1), (46, 42, 2),-- uniform = uni(一) + form(形状)

-- mov/mot (root_id=45)
(47, 45, 1),             -- move = mov(移动)
(48, 45, 1),             -- motion = mot(移动) + ion(名词)
(49, 32, 1), (49, 45, 2),-- promote = pro(向前) + mote(移动)
(50, 7, 1), (50, 45, 2), -- remote = re(回) + mote(移动)

-- pend (root_id=46)
(51, 33, 1), (51, 46, 2),-- depend = de(下) + pend(悬挂)
(52, 6, 1), (52, 46, 2), -- independent = in(不) + de(下) + pend(悬挂)
(53, 31, 1), (53, 46, 2),-- suspend = sus(下) + pend(悬挂)

-- sens/sent (root_id=47)
(54, 47, 1),             -- sense = sens(感觉)
(55, 47, 1),             -- sensitive = sens(感觉) + itive(形容词)

-- sign (root_id=48)
(56, 48, 1),             -- signal = sign(标记) + al(名词)
(57, 33, 1), (57, 48, 2),-- design = de(下) + sign(标记)

-- solv/solu (root_id=49)
(58, 49, 1),             -- solve = solv(解开)

-- tend/tens (root_id=50)
(59, 30, 1), (59, 50, 2),-- extend = ex(出) + tend(伸展)
(60, 50, 1);             -- attention = at(向) + tent(伸展) + ion(名词)