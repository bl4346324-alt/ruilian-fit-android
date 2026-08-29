package com.relifit.data.local

import androidx.room.withTransaction
import com.relifit.data.local.entity.Exercise
import com.relifit.data.local.entity.ExerciseEntry
import com.relifit.data.local.entity.WorkoutDay
import com.relifit.data.local.entity.WorkoutPlan

/**
 * 种子数据：首次启动写入 47 个动作（六大肌群 + 有氧/恢复）+ 7 套内置模板
 * 由 ReliFitApp 启动时调用，避免在 Room 回调中嵌套查询
 */
object SeedData {

    /** 整体在事务内执行；动作表/计划表分别判断（半初始化可被下次补齐） */
    suspend fun seedIfEmpty(db: AppDatabase) {
        db.withTransaction {
            if (db.exerciseDao().count() == 0) {
                db.exerciseDao().insertAll(seedExercises())
            }
            val exMap = db.exerciseDao().getAll().associate { it.name to it.id }
            if (db.planDao().count() == 0L) {
                seedPlans(db, exMap)
            }
        }
    }

    // ==================== 动作库种子（Demo 同款 25 个） ====================

    private fun seedExercises(): List<Exercise> {
        fun e(
            name: String, en: String, group: String, equip: String, diff: String,
            type: String, keys: String, errs: String, breath: String, off: Boolean = false
        ) = Exercise(
            name = name, nameEn = en, muscleGroup = group, equipment = equip,
            difficulty = diff, actionType = type,
            keyPoints = keys, mistakes = errs, breathTip = breath, offlineAvailable = off
        )
        return listOf(
            // ---------- 胸 ----------
            e("杠铃卧推", "Barbell Bench Press", "胸", "杠铃", "入门", "复合",
                "肩胛骨后缩下沉，收紧背部\n杠铃下放至胸部中线，肘部约 45°\n推起时呼气，胸肌主动收缩",
                "肘部过度外展，肩关节压力大\n杠铃弹胸借力，动作变形\n腰部过度反弓，失去核心稳定",
                "下放时吸气 2-3 秒控制离心，推起发力时呼气", true),
            e("上斜哑铃卧推", "Incline Dumbbell Press", "胸", "哑铃", "入门", "复合",
                "凳面 30° 左右，上胸发力\n哑铃沿弧线推起，顶端微内收\n全程控制下落，避免撞击",
                "凳面过陡，变成推肩\n顶端哑铃相撞，行程缩短\n耸肩借力",
                "下放吸气，推起呼气；顶端不锁死肘关节", true),
            e("俯卧撑", "Push-up", "胸", "自重", "入门", "复合",
                "身体成一条直线，核心收紧\n双手略宽于肩，下放至胸口接近地面\n推起时胸肌发力，不塌腰",
                "塌腰翘臀，腰椎受力\n只做半程\n肘部过度外展",
                "下放吸气，推起呼气；全程保持核心绷紧", true),
            e("哑铃飞鸟", "Dumbbell Fly", "胸", "哑铃", "中级", "孤立",
                "肘部微屈固定角度，像抱大树\n下放至胸部有拉伸感\n沿弧线夹起，顶峰收缩",
                "重量过大，变成卧推\n肘部角度变化，借力\n下放过低拉伤肩部",
                "下放缓慢吸气，夹起呼气，顶峰停 1 秒"),
            e("双杠臂屈伸", "Dips", "胸", "自重", "中级", "复合",
                "身体前倾刺激下胸\n下放至肩低于肘\n推起时胸肌收缩",
                "身体直立变成三头训练\n下放过深，肩部不适\n摆动借力",
                "下放吸气，推起呼气；肩部不适立即停止"),
            e("蝴蝶机夹胸", "Pec Deck Fly", "胸", "器械", "入门", "孤立",
                "调节座椅使把手与胸齐平\n夹起时顶峰收缩 1 秒\n缓慢还原控制离心",
                "用背发力夹\n行程过快，无控制\n耸肩",
                "夹起呼气，还原吸气，节奏平稳"),
            e("坐姿器械推胸", "Machine Chest Press", "胸", "器械", "入门", "复合",
                "背部贴紧靠垫，挺胸\n推至肘接近伸直\n还原时控制不砸片",
                "肩胛骨离开靠垫\n重量过大腰部代偿\n行程不足",
                "推起呼气，还原吸气，匀速控制"),
            // ---------- 背 ----------
            e("引体向上", "Pull-up", "背", "自重", "进阶", "复合",
                "肩胛骨先下沉，再发力上拉\n下巴过杠，肘部向下向后\n下放缓慢控制离心",
                "摆动借力\n只做半程\n耸肩代偿",
                "上拉呼气，下放吸气；顶峰停 1 秒"),
            e("高位下拉", "Lat Pulldown", "背", "器械", "入门", "复合",
                "挺胸，肩胛骨下沉\n拉至锁骨位置，肘部朝下\n控制离心还原",
                "身体后仰过大借力\n拉到颈后（肩部风险）\n耸肩",
                "下拉呼气，还原吸气，躯干稳定"),
            e("杠铃划船", "Barbell Row", "背", "杠铃", "中级", "复合",
                "髋部后移，躯干前倾约 45°\n杠铃拉至下腹，肘部贴身\n顶端肩胛骨夹紧",
                "弓腰驼背，腰椎受力\n用二头代偿\n躯干晃动",
                "拉起呼气，下放吸气；全程核心收紧"),
            e("单臂哑铃划船", "One-arm Dumbbell Row", "背", "哑铃", "入门", "复合",
                "单手支撑，背部平直\n哑铃拉至髋部，肘部贴身\n顶端停顿 1 秒",
                "身体旋转代偿\n耸肩\n重量过大轨迹变形",
                "拉起呼气，下放吸气，节奏匀速"),
            e("坐姿绳索划船", "Seated Cable Row", "背", "器械", "入门", "复合",
                "挺胸沉肩，肘部贴身后拉\n拉至腹部，肩胛骨夹紧\n控制离心还原",
                "弓背借力\n耸肩\n行程不足",
                "后拉呼气，还原吸气"),
            // ---------- 肩 ----------
            e("杠铃推举", "Overhead Press", "肩", "杠铃", "中级", "复合",
                "站距与肩同宽，核心收紧\n杠铃推至头顶正上方\n下放至锁骨高度",
                "腰部过度反弓\n肘部外展过度\n半程推举",
                "推起呼气，下放吸气；大重量注意护腰"),
            e("哑铃侧平举", "Dumbbell Lateral Raise", "肩", "哑铃", "入门", "孤立",
                "肘部微屈，向两侧平举至肩高\n小指略高于拇指（倒水姿势）\n缓慢下放控制",
                "耸肩借力\n甩动借惯性\n举过头顶",
                "上举呼气，下放吸气；轻重量多次数"),
            e("俯身飞鸟", "Bent-over Rear Fly", "肩", "哑铃", "中级", "孤立",
                "躯干前倾，背部平直\n双臂向两侧展开，挤压肩后束\n肘部微屈固定",
                "弓背\n用背阔肌代偿\n速度过快",
                "展开呼气，还原吸气"),
            // ---------- 腿 ----------
            e("杠铃深蹲", "Barbell Squat", "腿", "杠铃", "中级", "复合",
                "杠铃置于斜方肌，挺胸\n下蹲至大腿平行或更低\n膝盖与脚尖同向，全脚掌着地",
                "膝盖内扣\n脚跟离地\n弯腰弓背",
                "下蹲吸气，站起呼气；大重量佩戴护腰"),
            e("硬拉", "Deadlift", "腿", "杠铃", "进阶", "复合",
                "杠铃贴近小腿，背部平直\n髋膝同时发力站起\n顶端挺髋站直，不后仰",
                "弓腰启动，腰椎风险极高\n杠铃离身体太远\n顶端过度后仰",
                "起拉前深吸气绷紧核心，站起后呼气"),
            e("腿举", "Leg Press", "腿", "器械", "入门", "复合",
                "双脚与肩同宽踩踏板\n下放至膝盖约 90°\n推起时膝盖不锁死",
                "下放过深，腰部离垫\n膝盖内扣\n推起锁死膝盖",
                "下放吸气，推起呼气"),
            e("箭步蹲", "Lunge", "腿", "哑铃", "入门", "复合",
                "向前跨步，后膝接近地面\n前膝与脚尖同向\n身体保持竖直",
                "前膝超过脚尖过多\n身体前倾\n步幅过小",
                "下蹲吸气，起身呼气，交替进行"),
            // ---------- 手臂 ----------
            e("杠铃弯举", "Barbell Curl", "手臂", "杠铃", "入门", "孤立",
                "肘部固定在身体两侧\n弯举至前臂接近垂直\n下放缓慢控制",
                "身体后仰借力\n肘部前移\n半程弯举",
                "弯举呼气，下放吸气"),
            e("绳索下压", "Triceps Pushdown", "手臂", "器械", "入门", "孤立",
                "肘部固定贴身\n下压至手臂伸直\n顶端停顿挤压三头",
                "肘部外展\n身体前倾借力\n行程不足",
                "下压呼气，还原吸气"),
            e("锤式弯举", "Hammer Curl", "手臂", "哑铃", "入门", "孤立",
                "中立握位（掌心相对）\n肘部固定，弯举至前臂垂直\n下放控制",
                "摆动借力\n肘部前移\n重量过大",
                "弯举呼气，下放吸气"),
            // ---------- 核心 ----------
            e("平板支撑", "Plank", "核心", "自重", "入门", "孤立",
                "肘撑与肩同宽，身体成直线\n臀部收紧，腹部绷紧\n均匀呼吸不憋气",
                "塌腰\n撅臀\n憋气",
                "匀速呼吸，保持 30-60 秒"),
            e("卷腹", "Crunch", "核心", "自重", "入门", "孤立",
                "下背贴地，卷起上背\n手轻扶头部不拉颈\n顶峰收缩 1 秒",
                "用手拉脖子\n起身过高变仰卧起坐\n速度过快",
                "卷起呼气，还原吸气"),
            e("悬垂举腿", "Hanging Leg Raise", "核心", "自重", "进阶", "复合",
                "悬垂稳定，抬腿至与地面平行或更高\n下放缓慢控制\n避免摆动",
                "摆动借力\n只抬膝\n放腿过快",
                "抬腿呼气，下放吸气"),
            // ---------- 核心（补充） ----------
            e("俄罗斯转体", "Russian Twist", "核心", "自重", "中级", "孤立",
                "坐姿后仰，躯干与大腿约 45°\n双手左右转动，核心旋转发力\n脚可离地增加难度",
                "只用手臂摆动\n弓背塌腰\n速度过快失去控制",
                "转体呼气，还原吸气，节奏匀速"),
            e("侧平板支撑", "Side Plank", "核心", "自重", "中级", "孤立",
                "侧撑，肘在肩正下方\n身体成一条直线，髋部不塌\n保持稳定呼吸",
                "髋部下塌\n耸肩\n憋气",
                "匀速呼吸，每侧保持 30-60 秒"),
            e("臀桥", "Glute Bridge", "核心", "自重", "入门", "孤立",
                "仰卧屈膝，脚跟踩地\n臀部发力顶起至肩髋膝成直线\n顶端夹紧臀部停 1 秒",
                "腰部代偿过度反弓\n顶部不夹臀\n下放过快",
                "顶起呼气，下放吸气"),
            e("鸟狗式", "Bird Dog", "核心", "自重", "入门", "孤立",
                "四足跪姿，核心收紧\n对侧手脚同时伸出\n保持躯干稳定不旋转",
                "身体旋转\n塌腰\n抬腿过高",
                "伸展时吸气，收回呼气，动作缓慢"),
            e("死虫式", "Dead Bug", "核心", "自重", "中级", "孤立",
                "仰卧，四肢朝上，下背贴地\n对侧手脚缓慢下放\n保持下背始终贴地",
                "下背离地\n动作过快\n憋气",
                "下放吸气，收回呼气，全程慢速"),
            e("仰卧抬腿", "Lying Leg Raise", "核心", "自重", "入门", "孤立",
                "仰卧，双手放体侧\n双腿伸直缓慢抬至垂直\n下放不触地",
                "腰部反弓离地\n利用惯性甩腿\n放腿过快",
                "抬腿呼气，下放吸气，控制离心"),
            // ---------- 有氧 ----------
            e("跑步", "Running", "有氧", "自重", "入门", "有氧",
                "身体微微前倾，核心收紧\n步幅适中，前脚掌或全脚掌着地\n摆臂自然，配合呼吸节奏",
                "步幅过大伤膝\n脚跟重砸地面\n含胸驼背",
                "两步一呼两步一吸，保持匀速"),
            e("快走", "Brisk Walking", "有氧", "自重", "入门", "有氧",
                "抬头挺胸，目视前方\n摆臂幅度加大\n步频快于日常行走",
                "低头看手机\n摆臂僵硬\n步幅过小",
                "自然呼吸，保持节奏"),
            e("跳绳", "Jump Rope", "有氧", "自重", "入门", "有氧",
                "前脚掌着地，膝盖微屈\n手腕发力摇绳，大臂贴近身体\n跳跃高度越低越好",
                "整个脚掌砸地\n手臂大幅甩动\n跳得过高",
                "均匀呼吸，一组 1 分钟起步"),
            e("开合跳", "Jumping Jack", "有氧", "自重", "入门", "有氧",
                "双脚跳开与肩同宽，双手上举过头\n跳回时双手回落体侧\n落地缓冲屈膝",
                "落地过重\n手臂幅度不足\n节奏过快",
                "有节奏呼吸，落地时呼气"),
            e("高抬腿", "High Knees", "有氧", "自重", "入门", "有氧",
                "原地跑动，膝盖抬至髋部高度\n前脚掌着地\n摆臂配合，核心收紧",
                "身体后仰\n抬腿高度不足\n脚掌重砸地",
                "匀速呼吸，保持节奏"),
            e("波比跳", "Burpee", "有氧", "自重", "进阶", "有氧",
                "下蹲撑地，双脚后跳成平板\n快速收回双脚并向上跳起\n落地屈膝缓冲",
                "平板时塌腰\n跳起高度不足\n落地过重",
                "动作连贯，向上跳起时呼气"),
            e("登山跑", "Mountain Climber", "有氧", "自重", "中级", "有氧",
                "平板支撑位，核心收紧\n双腿交替快速向胸口提膝\n身体保持一条直线",
                "臀部抬起过高\n塌腰\n提膝幅度小",
                "快速呼吸，保持节奏"),
            e("划船机", "Rowing Machine", "有氧", "器械", "入门", "有氧",
                "蹬腿-后仰-拉桨顺序发力\n拉至肋骨下方，肘部贴身体\n回程先送臂再屈膝",
                "顺序混乱\n弓背拉桨\n膝盖过早弯曲",
                "拉桨呼气，回程吸气，保持匀速"),
            e("动感单车", "Spinning", "有氧", "器械", "入门", "有氧",
                "调节座椅使膝盖微屈踩到最低点\n核心收紧，上身稳定\n阻力适中保持踏频",
                "座椅过低伤膝\n站立骑行时重心压前\n阻力过大踏频过慢",
                "匀速呼吸，保持 80-100 踏频"),
            // ---------- 恢复 ----------
            e("全身拉伸", "Full Body Stretch", "恢复", "自重", "入门", "恢复",
                "动作缓慢，拉伸感明显但不过度疼痛\n每个部位保持 15-30 秒\n配合深呼吸放松",
                "弹震式拉伸\n憋气\n过度用力疼痛",
                "拉伸时缓慢呼气，感受肌肉放松"),
            e("泡沫轴放松", "Foam Rolling", "恢复", "自重", "入门", "恢复",
                "滚压大腿/背部/小腿等大肌群\n每次滚动缓慢，痛点停留 20-30 秒\n身体支撑稳定",
                "滚压速度过快\n直接压骨头\n憋气",
                "均匀呼吸，痛点处缓慢呼气"),
            e("猫牛式", "Cat-Cow", "恢复", "自重", "入门", "恢复",
                "四足跪姿，吸气塌腰抬头（牛式）\n呼气弓背低头（猫式）\n配合呼吸缓慢流动",
                "动作幅度过大\n屏气\n耸肩",
                "吸气牛式，呼气猫式，循环 10 次"),
            e("下犬式", "Downward Dog", "恢复", "自重", "入门", "恢复",
                "双手撑地，臀部上顶成倒 V\n脚跟尽量踩地，背部延展\n头部放松在两臂之间",
                "弓背耸肩\n脚跟悬空过高\n重心压向手腕",
                "缓慢均匀呼吸，保持 30 秒"),
            e("婴儿式", "Child's Pose", "恢复", "自重", "入门", "恢复",
                "跪坐，臀部坐向脚跟\n身体前倾，额头贴地\n手臂向前伸展放松",
                "臀部离脚跟\n耸肩\n用力过度",
                "缓慢深呼吸，放松保持 30 秒"),
            e("腿部拉伸", "Leg Stretch", "恢复", "自重", "入门", "恢复",
                "站立或坐姿，缓慢拉伸大腿前后侧与小腿\n保持呼吸，拉伸感适中\n左右交替",
                "弹震拉伸\n憋气\n过度疼痛",
                "拉伸时缓慢呼气，保持 20-30 秒"),
            e("肩颈放松", "Neck & Shoulder Release", "恢复", "自重", "入门", "恢复",
                "缓慢转动颈部与耸肩绕环\n双手交叉拉伸肩部\n动作轻柔缓慢",
                "快速甩头\n过度用力\n耸肩紧张",
                "匀速呼吸，动作轻柔")
        )
    }

    // ==================== 4 套内置模板种子（与 Demo 一致） ====================

    /** 动作条目快捷构造：名称、组数、次数、休息秒数 */
    private fun item(name: String, sets: Int, reps: Int, rest: Int): Triple<String, Pair<Int, Int>, Int> =
        Triple(name, sets to reps, rest)

    private suspend fun seedPlans(db: AppDatabase, exMap: Map<String, Long>) {
        val now = System.currentTimeMillis()

        // ① 力量提升（Demo 计划详情页同款：周一推 / 周三蹲 / 周五拉）
        val plan1Id = db.planDao().insertPlan(
            WorkoutPlan(name = "力量提升计划", type = "力量", isTemplate = true, cycleWeeks = 4, targetDurationMin = 60, daysPerWeek = 3, createdAt = now, updatedAt = now)
        )
        seedDay(db, plan1Id, 1, "上肢推日", 90, listOf(
            item("杠铃卧推", 3, 8, 90), item("上斜哑铃卧推", 3, 10, 60),
            item("哑铃飞鸟", 3, 12, 60), item("俯卧撑", 3, 15, 60),
            item("蝴蝶机夹胸", 3, 12, 60)
        ), exMap)
        seedDay(db, plan1Id, 3, "下肢蹲日", 120, listOf(
            item("杠铃深蹲", 5, 5, 120), item("腿举", 4, 10, 90),
            item("箭步蹲", 3, 12, 60), item("卷腹", 3, 15, 60)
        ), exMap)
        seedDay(db, plan1Id, 5, "上肢拉日", 90, listOf(
            item("高位下拉", 4, 10, 90), item("杠铃划船", 4, 8, 90),
            item("单臂哑铃划船", 3, 12, 60), item("杠铃弯举", 3, 12, 60)
        ), exMap)

        // ② 新手增肌（全身分化 3 练）
        val plan2Id = db.planDao().insertPlan(
            WorkoutPlan(name = "新手增肌", type = "力量", isTemplate = true, cycleWeeks = 8, targetDurationMin = 45, daysPerWeek = 3, createdAt = now, updatedAt = now)
        )
        seedDay(db, plan2Id, 1, "全身训练 A", 60, listOf(
            item("俯卧撑", 3, 12, 60), item("高位下拉", 3, 10, 60),
            item("箭步蹲", 3, 10, 60), item("哑铃侧平举", 3, 12, 60)
        ), exMap)
        seedDay(db, plan2Id, 3, "全身训练 B", 60, listOf(
            item("坐姿器械推胸", 3, 10, 60), item("单臂哑铃划船", 3, 10, 60),
            item("腿举", 3, 12, 60), item("卷腹", 3, 15, 60)
        ), exMap)
        seedDay(db, plan2Id, 5, "全身训练 C", 60, listOf(
            item("上斜哑铃卧推", 3, 10, 60), item("坐姿绳索划船", 3, 10, 60),
            item("杠铃深蹲", 3, 8, 90), item("平板支撑", 3, 60, 30)
        ), exMap)

        // ③ 减脂燃脂（短休高频）
        val plan3Id = db.planDao().insertPlan(
            WorkoutPlan(name = "减脂燃脂", type = "有氧", isTemplate = true, cycleWeeks = 4, targetDurationMin = 40, daysPerWeek = 4, createdAt = now, updatedAt = now)
        )
        seedDay(db, plan3Id, 1, "燃脂全身 A", 45, listOf(
            item("俯卧撑", 4, 15, 45), item("箭步蹲", 4, 12, 45),
            item("卷腹", 4, 20, 30), item("哑铃侧平举", 4, 15, 30)
        ), exMap)
        seedDay(db, plan3Id, 2, "燃脂全身 B", 45, listOf(
            item("杠铃深蹲", 4, 12, 45), item("俯卧撑", 4, 15, 45),
            item("平板支撑", 4, 60, 30), item("绳索下压", 4, 15, 30)
        ), exMap)
        seedDay(db, plan3Id, 4, "燃脂全身 C", 45, listOf(
            item("杠铃卧推", 4, 10, 60), item("杠铃划船", 4, 10, 60),
            item("箭步蹲", 4, 12, 45), item("悬垂举腿", 4, 10, 30)
        ), exMap)
        seedDay(db, plan3Id, 6, "燃脂全身 D", 45, listOf(
            item("坐姿器械推胸", 4, 12, 45), item("高位下拉", 4, 12, 45),
            item("卷腹", 4, 20, 30), item("哑铃飞鸟", 4, 15, 45)
        ), exMap)

        // ④ 居家无器械（全自重）
        val plan4Id = db.planDao().insertPlan(
            WorkoutPlan(name = "居家无器械", type = "核心", isTemplate = true, cycleWeeks = 4, targetDurationMin = 30, daysPerWeek = 3, createdAt = now, updatedAt = now)
        )
        seedDay(db, plan4Id, 1, "居家训练 A", 45, listOf(
            item("俯卧撑", 4, 12, 45), item("平板支撑", 3, 60, 30),
            item("卷腹", 3, 20, 30), item("箭步蹲", 3, 12, 45)
        ), exMap)
        seedDay(db, plan4Id, 3, "居家训练 B", 45, listOf(
            item("双杠臂屈伸", 3, 10, 60), item("俯卧撑", 4, 12, 45),
            item("悬垂举腿", 3, 10, 45), item("平板支撑", 3, 60, 30)
        ), exMap)
        seedDay(db, plan4Id, 5, "居家训练 C", 45, listOf(
            item("俯卧撑", 4, 15, 45), item("箭步蹲", 3, 12, 45),
            item("卷腹", 3, 20, 30), item("平板支撑", 3, 60, 30)
        ), exMap)

        // ⑤ 核心强化（type=核心）
        val plan5Id = db.planDao().insertPlan(
            WorkoutPlan(name = "核心强化", type = "核心", isTemplate = true, cycleWeeks = 4, targetDurationMin = 30, daysPerWeek = 3, createdAt = now, updatedAt = now)
        )
        seedDay(db, plan5Id, 1, "核心稳定 A", 45, listOf(
            item("平板支撑", 3, 60, 45), item("卷腹", 4, 15, 45),
            item("俄罗斯转体", 3, 20, 45), item("臀桥", 3, 15, 45)
        ), exMap)
        seedDay(db, plan5Id, 3, "核心力量 B", 45, listOf(
            item("悬垂举腿", 3, 10, 60), item("死虫式", 3, 12, 45),
            item("鸟狗式", 3, 12, 45), item("仰卧抬腿", 3, 15, 45)
        ), exMap)
        seedDay(db, plan5Id, 5, "核心耐力 C", 45, listOf(
            item("侧平板支撑", 3, 45, 45), item("登山跑", 4, 20, 45),
            item("卷腹", 4, 20, 30), item("平板支撑", 3, 60, 45)
        ), exMap)

        // ⑥ 有氧燃脂（type=有氧）
        val plan6Id = db.planDao().insertPlan(
            WorkoutPlan(name = "有氧燃脂", type = "有氧", isTemplate = true, cycleWeeks = 4, targetDurationMin = 35, daysPerWeek = 4, createdAt = now, updatedAt = now)
        )
        seedDay(db, plan6Id, 1, "有氧 A · 节奏", 30, listOf(
            item("快走", 1, 30, 30), item("开合跳", 3, 30, 30), item("高抬腿", 3, 30, 30)
        ), exMap)
        seedDay(db, plan6Id, 2, "有氧 B · 燃脂", 30, listOf(
            item("跳绳", 5, 60, 30), item("开合跳", 3, 30, 30), item("高抬腿", 3, 30, 30)
        ), exMap)
        seedDay(db, plan6Id, 4, "有氧 C · 高强度", 45, listOf(
            item("波比跳", 4, 10, 45), item("登山跑", 4, 20, 45), item("划船机", 4, 10, 60)
        ), exMap)
        seedDay(db, plan6Id, 6, "有氧 D · 恢复跑", 30, listOf(
            item("跑步", 1, 30, 30), item("开合跳", 3, 30, 30), item("跳绳", 3, 60, 30)
        ), exMap)

        // ⑦ 恢复放松（type=恢复）
        val plan7Id = db.planDao().insertPlan(
            WorkoutPlan(name = "恢复放松", type = "恢复", isTemplate = true, cycleWeeks = 4, targetDurationMin = 25, daysPerWeek = 3, createdAt = now, updatedAt = now)
        )
        seedDay(db, plan7Id, 1, "拉伸放松 A", 30, listOf(
            item("全身拉伸", 1, 15, 30), item("猫牛式", 3, 10, 30),
            item("下犬式", 3, 30, 30), item("肩颈放松", 1, 10, 30)
        ), exMap)
        seedDay(db, plan7Id, 3, "筋膜放松 B", 30, listOf(
            item("泡沫轴放松", 1, 15, 30), item("婴儿式", 3, 30, 30),
            item("腿部拉伸", 3, 30, 30), item("全身拉伸", 1, 15, 30)
        ), exMap)
        seedDay(db, plan7Id, 5, "拉伸放松 C", 30, listOf(
            item("下犬式", 3, 30, 30), item("猫牛式", 3, 10, 30),
            item("婴儿式", 3, 30, 30), item("泡沫轴放松", 1, 15, 30)
        ), exMap)
    }

    private suspend fun seedDay(
        db: AppDatabase, planId: Long, dayIndex: Int, name: String, rest: Int,
        items: List<Triple<String, Pair<Int, Int>, Int>>,
        exMap: Map<String, Long>
    ) {
        val dayId = db.planDao().insertDay(
            WorkoutDay(planId = planId, dayIndex = dayIndex, name = name, defaultRestSec = rest)
        )
        items.forEachIndexed { idx, (exName, setsReps, restSec) ->
            val exId = exMap[exName] ?: return@forEachIndexed
            db.planDao().insertEntry(
                ExerciseEntry(
                    workoutDayId = dayId, exerciseId = exId, sortOrder = idx,
                    targetSets = setsReps.first, targetReps = setsReps.second, restSec = restSec
                )
            )
        }
    }
}
