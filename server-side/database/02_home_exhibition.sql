-- ============================================
-- ciTY展馆 — 首页展览模块建表 + 预置数据
-- 数据库：city_art
-- ============================================

-- ========== 1. 美术馆表 ==========
DROP TABLE IF EXISTS `gallery`;
CREATE TABLE `gallery` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`             VARCHAR(100)    NOT NULL                COMMENT '美术馆名称',
    `cover_image`      VARCHAR(255)    DEFAULT NULL            COMMENT '封面图片URL',
    `address`          VARCHAR(255)    NOT NULL                COMMENT '详细地址',
    `city`             VARCHAR(50)     NOT NULL                COMMENT '所在城市（如：南京）',
    `district`         VARCHAR(50)     DEFAULT NULL            COMMENT '所在区/县（如：秦淮区）',
    `intro`            TEXT            DEFAULT NULL            COMMENT '美术馆简介',
    `exhibition_count` INT             NOT NULL DEFAULT 0      COMMENT '当前在展数量（冗余字段，方便首页直接读取，避免COUNT查询）',
    `status`           TINYINT         NOT NULL DEFAULT 0      COMMENT '状态：0-正常营业 1-闭馆维护',
    `type`             INT             NOT NULL DEFAULT 0      COMMENT '美术馆类型（0/1/2... 数字表示）',
    `create_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    KEY `idx_city` (`city`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='美术馆信息表';

-- ========== 2. 展览表 ==========
DROP TABLE IF EXISTS `exhibition`;
CREATE TABLE `exhibition` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `gallery_id`    BIGINT UNSIGNED NOT NULL                COMMENT '所属美术馆ID（关联 gallery.id）',
    `title`         VARCHAR(200)    NOT NULL                COMMENT '展览标题',
    `subtitle`      VARCHAR(255)    DEFAULT NULL            COMMENT '副标题/英文标题',
    `poster_image`  VARCHAR(255)    NOT NULL                COMMENT '海报/封面图URL',
    `start_date`    DATE            NOT NULL                COMMENT '开始日期',
    `end_date`      DATE            NOT NULL                COMMENT '结束日期',
    `description`   TEXT            DEFAULT NULL            COMMENT '展览详情介绍',
    `is_hot`        TINYINT         NOT NULL DEFAULT 0      COMMENT '是否热门/轮播展示：0-否 1-是',
    `sort_order`    INT             NOT NULL DEFAULT 0      COMMENT '排序权重（数字越大越靠前）',
    `status`        TINYINT         NOT NULL DEFAULT 0      COMMENT '状态：0-未开始 1-进行中 2-已结束',
    `type`          INT             NOT NULL DEFAULT 0      COMMENT '展览类型（0/1/2... 数字表示）',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    KEY `idx_gallery` (`gallery_id`),
    KEY `idx_dates` (`start_date`, `end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='展览信息表';

-- ========== 3. Banner表 ==========
DROP TABLE IF EXISTS `banner`;
CREATE TABLE `banner` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `image_url`   VARCHAR(500)    NOT NULL                COMMENT 'Banner图片地址',
    `title`       VARCHAR(100)    DEFAULT ''              COMMENT '标题（可选，看前端要不要在图上叠字）',
    `sort`        INT             NOT NULL DEFAULT 0      COMMENT '排序（数字越大越靠前）',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='首页Banner表';

-- ============================================
-- 预置数据
-- ============================================

-- ============================================
-- 预置美术馆（12家：南京8 + 上海2 + 北京2）
-- ============================================
INSERT INTO `gallery` (`id`, `name`, `cover_image`, `address`, `city`, `district`, `intro`, `exhibition_count`, `status`, `type`)
VALUES
-- 南京（8家）
(1, '江苏省美术馆', '/static/mock/gallery_jiangsu.jpg', '南京市玄武区长江路333号', '南京', '玄武区', '江苏省美术馆始建于1936年，是中国近代第一座国家级美术馆，馆藏以近现代美术作品为主，涵盖中国画、油画、版画、雕塑等多个门类。', 2, 0, 0),
(2, '德基美术馆', '/static/mock/gallery_deji.jpg', '南京市玄武区中山路18号德基广场8楼', '南京', '玄武区', '德基美术馆位于南京市中心德基广场，致力于推广当代艺术与国际交流，定期举办国内外知名艺术家个展及群展。', 2, 0, 1),
(3, '四方当代美术馆', '/static/mock/gallery_sifang.jpg', '南京市浦口区珍七路9号(佛手湖)', '南京', '浦口区', '四方当代美术馆由美国建筑师Steven Holl设计，坐落于老山森林公园佛手湖畔，建筑本身即是一件艺术品。', 1, 0, 0),
(4, '南京博物院艺术馆', '/static/mock/gallery_nanbo.jpg', '南京市玄武区中山东路321号', '南京', '玄武区', '南京博物院是中国三大博物馆之一，艺术馆常设历代绘画、书法、雕塑等专题陈列，定期引进国内外重磅特展。', 1, 0, 0),
(5, '金陵美术馆', '/static/mock/gallery_jinling.jpg', '南京市秦淮区剪子巷50号', '南京', '秦淮区', '金陵美术馆坐落于老门东历史文化街区，由原南京色织厂厂房改造而成，专注于南京本地当代艺术生态的培育与展示。', 1, 0, 1),
(6, '南艺美术馆', '/static/mock/gallery_nanyi.jpg', '南京市鼓楼区虎踞北路15号', '南京', '鼓楼区', '南京艺术学院美术馆依托百年南艺的学术底蕴，以实验性、先锋性展览见长，是青年艺术家的重要展示平台。', 1, 0, 0),
(7, '紫金当代艺术馆', '/static/mock/gallery_zijin.jpg', '南京市建邺区江东中路369号', '南京', '建邺区', '紫金当代艺术馆位于河西新城核心地段，致力于当代油画、装置及新媒体艺术的收藏与推广，展厅面积逾3000平米。', 1, 0, 1),
(8, '瞻园美术馆', '/static/mock/gallery_zhanyuan.jpg', '南京市秦淮区瞻园路128号', '南京', '秦淮区', '瞻园美术馆藏身于明代古典园林瞻园之侧，以中国传统书画与工艺美术展览为特色，将园林美学与展陈空间融为一体。', 1, 0, 0),
-- 上海（2家）
(9, '上海当代艺术博物馆', '/static/mock/gallery_psa.jpg', '上海市黄浦区花园港路200号', '上海', '黄浦区', '上海当代艺术博物馆（PSA）是中国大陆第一家公立当代艺术博物馆，由原南市发电厂改建而成，是上海双年展的主展馆。', 1, 0, 1),
(10, '龙美术馆（西岸馆）', '/static/mock/gallery_long.jpg', '上海市徐汇区龙腾大道3398号', '上海', '徐汇区', '龙美术馆由收藏家刘益谦、王薇夫妇创办，西岸馆以"伞拱"结构为特色，专注于当代艺术与传统文化对话。', 1, 0, 1),
-- 北京（2家）
(11, 'UCCA尤伦斯当代艺术中心', '/static/mock/gallery_ucca.jpg', '北京市朝阳区酒仙桥路4号798艺术区', '北京', '朝阳区', 'UCCA是中国领先的当代艺术机构，位于798艺术区核心地带，每年呈现十余场重要展览及数百场公共项目。', 1, 0, 1),
(12, '中国美术馆', '/static/mock/gallery_namoc.jpg', '北京市东城区五四大街1号', '北京', '东城区', '中国美术馆始建于1958年，是新中国成立后建设的第一座国家造型艺术博物馆，馆藏各类美术作品13万余件。', 1, 0, 0);

-- ============================================
-- 预置展览（12场：南京8 + 上海2 + 北京2）
-- ============================================
INSERT INTO `exhibition` (`id`, `gallery_id`, `title`, `subtitle`, `poster_image`, `start_date`, `end_date`, `description`, `is_hot`, `sort_order`, `status`, `type`)
VALUES
-- 南京（8场）
(1, 1, '印象派百年回顾', 'Century of Impressionism', '/static/mock/exhibition_impressionism.jpg', '2026-06-01', '2026-09-30', '展出莫奈、雷诺阿、德加、马奈等印象派大师真迹60余幅，从法国奥赛博物馆等国际顶级机构借展，是国内近年来规模最大的印象派专题展。', 1, 100, 1, 1),
(2, 1, '新金陵画派文献展', 'Documents of New Jinling School', '/static/mock/exhibition_jinling.jpg', '2026-07-01', '2026-10-15', '以傅抱石、钱松喦、亚明、宋文治、魏紫熙等新金陵画派代表人物为核心，展出原作80余件及珍贵手稿、信札等文献资料。', 0, 50, 1, 0),
(3, 2, '流动的边界——新媒体艺术展', 'Flowing Boundaries', '/static/mock/exhibition_newmedia.jpg', '2026-08-01', '2026-11-30', '汇集全球12位新媒体艺术家的装置、影像、交互作品，探讨数字时代人与空间、时间的全新关系。沉浸式体验占比超过70%。', 1, 90, 0, 1),
(4, 2, '赵无极：无境之境', 'Zao Wou-Ki: Infinite Horizons', '/static/mock/exhibition_zaowouki.jpg', '2026-07-15', '2026-12-31', '集中呈现赵无极先生1950年代至2000年代油画、水彩及版画作品40余件，完整回顾其从具象到抽象的创作演变。', 0, 60, 1, 0),
(5, 4, '大宋风雅——宋代文物精品展', 'Elegance of Song Dynasty', '/static/mock/exhibition_song.jpg', '2026-04-01', '2026-08-31', '展出瓷器、书画、金银器等宋代文物精品200余件，从文人四雅（点茶、焚香、插花、挂画）切入，全景再现宋代生活美学。', 1, 85, 1, 0),
(6, 5, '老门东·新青年——南京青年艺术家联展', 'New Youth of Old Nanjing', '/static/mock/exhibition_youth.jpg', '2026-07-01', '2026-10-31', '集结南京本地15位35岁以下青年艺术家，涵盖油画、版画、综合材料、影像等多种媒介，呈现新生代的在地创作面貌。', 0, 40, 1, 0),
(7, 3, '建筑师的纸本世界', 'Architects on Paper', '/static/mock/exhibition_architect.jpg', '2026-05-15', '2026-09-30', '展出Steven Holl、王澍、妹岛和世等10位国际知名建筑师的草图、手绘、模型及影像，呈现建筑设计背后的思维过程。', 0, 30, 1, 0),
(8, 6, '时间的形状——中国当代雕塑邀请展', 'The Shape of Time', '/static/mock/exhibition_sculpture.jpg', '2026-06-15', '2026-09-15', '邀请隋建国、向京、展望等15位当代雕塑家，展出大型装置及雕塑作品30余件，探讨材料、空间与时间的三重关系。', 1, 70, 1, 0),
-- 上海（2场）
(9, 9, '第15届上海双年展——生态未来', '15th Shanghai Biennale: Eco Future', '/static/mock/exhibition_biennale.jpg', '2026-05-01', '2026-10-31', '本届双年展以"生态未来"为主题，邀请全球40余位艺术家从气候变迁、物种共生、科技伦理等角度展开创作，探讨人与自然的新型关系。', 1, 95, 1, 1),
(10, 10, '凝固的诗意——国际建筑摄影展', 'Poetry in Concrete', '/static/mock/exhibition_architecture.jpg', '2026-07-01', '2026-09-30', '展出Iwan Baan、Helene Binet等8位国际建筑摄影师的作品100余幅，以镜头语言重新解读当代建筑的空间叙事。', 0, 35, 1, 0),
-- 北京（2场）
(11, 11, '曹斐：时代舞台', 'Cao Fei: Staging the Era', '/static/mock/exhibition_caofei.jpg', '2026-06-01', '2026-11-30', '曹斐迄今为止规模最大的个展，涵盖影像、装置、虚拟现实等作品20余件，以持续二十年的创作串联起中国城市化进程中的众生相。', 1, 80, 1, 1),
(12, 12, '墨韵千秋——历代书法名作展', 'Eternal Ink: Masterpieces of Chinese Calligraphy', '/static/mock/exhibition_calligraphy.jpg', '2026-07-01', '2026-10-31', '从王羲之《兰亭序》唐代摹本到近现代大家真迹，展出历代书法名作80余件，串联起中国书法两千年的演变脉络。', 0, 45, 1, 0);

-- ============================================
-- 预置 Banner（10条）
-- ============================================
INSERT INTO `banner` (`id`, `image_url`, `title`, `sort`)
VALUES
(1, '/static/mock/banner_impressionism.jpg', '印象派百年回顾——江苏省美术馆', 100),
(2, '/static/mock/banner_biennale.jpg', '第15届上海双年展"生态未来"', 95),
(3, '/static/mock/banner_newmedia.jpg', '流动的边界——新媒体艺术沉浸式大展', 90),
(4, '/static/mock/banner_song.jpg', '大宋风雅——宋代文物精品展', 85),
(5, '/static/mock/banner_caofei.jpg', '曹斐：时代舞台——UCCA尤伦斯', 80),
(6, '/static/mock/banner_sculpture.jpg', '时间的形状——中国当代雕塑邀请展', 75),
(7, '/static/mock/banner_zaowouki.jpg', '赵无极：无境之境——德基美术馆', 70),
(8, '/static/mock/banner_calligraphy.jpg', '墨韵千秋——历代书法名作展', 60),
(9, '/static/mock/banner_jinling.jpg', '新金陵画派文献展', 55),
(10, '/static/mock/banner_nanjing.jpg', '2026南京艺术季——全城美术馆联展', 50);
