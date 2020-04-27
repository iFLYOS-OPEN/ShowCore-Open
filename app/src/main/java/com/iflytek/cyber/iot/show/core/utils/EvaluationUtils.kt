package com.iflytek.cyber.iot.show.core.utils

import android.text.TextUtils

class EvaluationUtils {

    private val chineseWordList = arrayListOf(
        "掐", "女", "丈", "埋", "损", "丁", "暖",
        "对", "促", "铜", "任", "卡", "古",
        "揭", "丸", "吞", "抓", "修", "荡", "旺", "摔", "脸", "热", "把", "耗",
        "平", "筛", "亏", "否", "瓦", "棍", "松", "匀",
        "女", "用", "喜", "说", "揣", "咱", "佛", "此", "勋", "院",
        "子", "钻", "牙", "民", "饭", "北", "何", "凋", "坡",
        "观", "骗", "荒", "烤", "赔", "级", "贪", "抢", "风", "理", "缤",
        "能", "锅", "邻", "口", "而", "翁", "掠", "羊", "思", "捏", "灾",
        "穷", "体", "描", "月", "容", "参", "歪",
        "续", "贴", "搜", "场", "拨", "是", "支", "窗", "目",
        "耻", "掘", "熬", "嘎", "舟", "纲", "押", "帘", "柠", "拽", "慌", "泉", "洒", "开", "揉",
        "昂", "别", "件", "揣", "脓", "群", "吓", "债", "产", "檬", "跌", "宾", "铺",
        "锐", "综", "迅", "扯", "柴", "删", "风", "夜", "心", "属", "最", "从",
        "源", "舍", "赔", "嫩", "坑", "条",
        "饮", "塑", "断", "女", "窘", "恶", "费", "狠", "笔", "脚", "亮", "垮", "暖", "绿",
        "琼", "池", "给", "坯", "要", "象", "画", "窜", "虐",
        "凶", "狮", "贸", "方", "邸", "牛", "洋", "博", "顿", "思", "烤", "浪", "下",
        "六", "艇", "某", "托",
        "润", "匡", "缺", "捐", "俩", "您", "惨", "肯", "筒", "肿", "疼", "平", "仍", "覃"
    )

    private val chineseWordsList = arrayListOf(
        "迥然", "虐政", "可观", "旅伴", "谱写", "女婿", "老头儿", "高原", "摘要", "迷信", "摧残",
        "婆家", "犬马", "穷酸", "贴切", "否则", "衬衫", "能够", "作废", "吹牛", "调查", "仇恨", "刚才", "软件",
        "怀念", "军饷", "丁零", "主编", "差点儿", "朋友", "哈哈", "亲爱", "爽快", "花纹", "粗粮", "狂妄", "东风",
        "角色", "揣测", "刷子", "聊天儿", "篮球", "寒冷", "航空", "下来", "阔气", "捆绑", "浑身", "没事儿", "巡逻",
        "凉粉", "旅途", "挂号", "存在", "捐款", "僧侣", "老头儿", "日常", "罪责", "晒台", "衰弱", "快乐", "学会", "补贴",
        "散漫", "参加", "面条儿", "温柔", "喧闹", "牛皮", "窘况", "雄伟", "军队", "群众", "穷人", "省长", "女性", "刀把儿",
        "别的", "压迫", "民兵", "马虎", "权贵", "觉悟", "确定", "困难", "理睬", "揣测", "童话", "酿造"
    )

    private val chineseSentenceList = arrayListOf(
        "登月太空飞船飞行时拍摄的照片显示出，地球是一个表面由蓝色和白色构成的美丽的球体。",
        "夜从浓浓的蓝黑逐渐浅淡，终于变成带点银灰的乳色。",
        "时常,夏日清晨的鸟唱是最轻悄的，像是不忍吵醒那将睡的夜,像是牵挂着那将要隐入林后的夜风。",
        "清晨的林术有醉人的清香。那么醇，那么厚，使你不由自主地深深呼吸、闻嗅吸饮那如洒的甘冽。",
        "他的脸上呈现出一个悲剧，一张涵蓄了许多愁苦和力量的脸。",
        "瞧，它多美丽，娇巧的小嘴，啄理着绿色的羽毛，鸭子样的扁脚，呈现出春草的鹅黄。",
        "控制紧张情绪的最佳做法是选择你有所了解并感兴趣的话题。",
        "爷爷说，只有那不畏艰险的勇士，才能走进大海的世界，得到幸福和安宁。",
        "我盼着长大的一天，骑着骏马，去寻找那迷人的大海。",
        "在我的后园，可以看见墙外有两株树，一株是枣树，还有一株也是枣树。",
        "人若能知足，虽贫不苦;若能安分，虽失意不苦;",
        "母亲本不愿出来的，她老了，身体不好，走远一点儿就觉得很累。",
        "那里有金色的菜花，两行整齐的桑树，尽头一口水波粼粼的鱼塘。",
        "落叶在欢迎早起者的脚步， 要他们快来欣赏树梢头那更多的新叶 ， 与它们身边初放的晨花 。",
        "北京的导游跑断腿，西安的导游磨破嘴，海南的导游晒脱水。",
        "童年的记忆是河边的那片空地，撒欢儿似的疯跑，欢笑着的追赶，摔跤后的哭喊。",
        "秋冬之季，小草熟睡，绿树脱衣，只有松柏依然挺拔。",
        "人类赖以生存和发展的环境受到了严峻挑战，生态环境遭到了严重破坏，各种污染事故频频发生。",
        "如果二十岁的心像小溪，三十岁的心像小河，四十岁的心像条江。那么，五十岁的心就是大海。",
        "成功也并非只有坚持这一条路，适时的认输，适时的放弃，懂得变通，同样也是一种成功智慧。",
        "人生本来就是这样，我们如今所遭遇的，放不下的，不过是漫长生命里的沧海一粟。",
        "雨还在下着，将操场清刷得一片清新，绿色的草坪，红色的跑道，赏心悦目。",
        "回忆，不会仅仅是酸涩的泪水和离情别绪，一如时间之琼酿，总是越陈越香。",
        "一池碧蓝的湖水，水鸟贴着水面飞翔，水边金黄色的芦苇草随风轻轻摇摆。",
        "流淌的夜色，一如既往的深沉，被月光覆盖的街道，忧伤与思念共舞，洒满了我孤寂的心灵。",
        "我敞开胸襟，呼吸着海香很浓的风，开始领略书本里汹涌的内容，澎湃的情思，伟大而深邃的哲理。",
        "也就在这个时候，喜悦突然象涌上海面的潜流，滚过我的胸间，使我暗暗地激动。",
        "一阵风吹来，树枝轻轻地摇晃，美丽的银条儿和雪球儿簇簇地落下来，玉屑似的雪末儿随风飘扬；",
        "客人带着好像敬畏又好像怜惜的神情，默不作声地望着他。"
    )

    private val englishWordList = arrayListOf(
        "volume", "excess", "bake", "dense", "vague", "inference", "chimney", "flexibility ",
        "intensify", "weld", "chop", "thrill", "defy", "heave", "forum", "architecture",
        "thermos", "filmmaker", "consent", "cereal",
        "pit", "grace", "mystery", "undergo", "medium", "existence", "elect",
        "accuse", "exclusive", "constitute", "depend", "general",
        "venture", "recover", "clap", "contain", "continent", "vain", "indifferent"
        , "universe", "ashamed", "vivid", "bakery", "cost",
        "soar", "extraordinary", "conservative", "bold", "classify", "magnetic",
        "craft", "exaggerate", "rid", "spur", "frequency",
        "delivery", "resume", "alternative", "violence", "intensity", "portable",
        "eliminate", "plunge", "impulse", "funeral",
        "majority", "bend", "triangle", "slender", "scout", "furnace", "brow",
        "fluent", "distress", "excess", "agent", "appropriate",
        "transfer", "rarely", "headline", "emphasize", "interval", "interfere",
        "community", "contemporary", "demonstrate",
        "beneficial", "appearance", "undertake", "mental", "worthwhile", "prior",
        "invitation", "proud", "undergraduate",
        "prospect", "source", "infect", "scheme", "breeze", "lane", "corruption",
        "dominant", "fuss", "tanker", "plausible",
        "illustration", "horsepower", "radar", "deck", "struggle", "bolt", "weave",
        "atomic", "stale", "astronomer", "prime",
        "halt", "plague", "torture", "diagram", "anticipate", "modify", "protective", "relate"
    )

    private val englishSentenceList = arrayListOf(
        "Would you please open the door for me?",
        "May I ask you a question?",
        "Get me my coat, please.",
        "Make me a cup of coffee, will you?",
        "Call me tomorrow if you have time.",
        "I don’t feel very well.",
        "He’s got a bad headache.",
        "It’s bleeding. You’d better see a doctor about that cut.",
        "Take two pills and have a good rest.",
        "He was absent yesterday. Do you know why?",
        "What you have said about this is very interesting.",
        "There are always two sides to everything.",
        "I don't know for sure.",
        "I've done my best.",
        "What’s wrong with you?",
        "Today we are going to learn some new words.",
        "It’s time to go to bed. ",
        "You’re making progress everyday.",
        "Excuse me, what’s the time, please? ",
        "I like to draw pictures there.",
        "I would like to talk to you for a minute.",
        "I didn’t mean to offend you.",
        "When he was young, he liked to try out new ideas.",
        "We can not live without air. ",
        "Tom has never done such a thing.",
        "Why did Jim go to the hospital yesterday?",
        "Ability may get you to the top, but it takes character to keep you there.",
        "The important thing in life is to have a great aim, and the determination to attain it. ",
        "I enjoy warm in time. I forget blooms in the internal.",
        "The fingers will not move, tears will not flow, the time will not go.",
        "I want to be strong that nothing can disturb your peace of mind.",
        "The time is so precious, just a second toilet by other people robbed.",
        "They never seem to say goodbye. And every time is never.",
        "Most people want to transform the world, but few people want to change their own.",
        "Not caring, is precisely the existence of respect for others.",
        "When you use knowledge, you will find you haven't enough.",
        "If you keep bothering me, I shall have to dismiss you."
    )

    private val englishArticleList = arrayListOf(
        "Youth is not a time of life; it is a state of mind. It is not a matter of rosy cheeks, red lips and supple knees. It is a matter of the will, a quality of the imagination, vigor of the emotions; it is the freshness of the deep spring of life.",
        "The first drops of rain are huge; they split into the dust on the ground, and plunk on the roof. The rain now becomes a torrent, flung by a rising wind. Together they batter the trees and level the grasses. Water streams off roofs.",
        "The most beautiful part of spring is sawn. The east side of the sky turns into a grayish color. From the green trees, the chirping of the birds can be heard. The grass is soft and green on the side of the road, they quietly welcome mother earth to wake up.",
        "A good book may be among the best of friends. It is the same today that it always was, and it will never change. It is the most patient and cheerful of companions. It does not turn its back upon us in times of adversity or distress.",
        "Since I was a child I dream to be a scientist. I think a good scientist can make the world change a lot. If I become a scientist, I will make the desert cover with green trees, make the war never take places and make disaster get far away from people.",
        "You can try to write your objective on paper and make some plans to achieve it.In this way,you will know how to arrange your time and to spend your time properly.",
        """We Must Face Failure.As we all know, "Failure is the mother of success." But few people can really understand what the saying means.In the world,  In fact, failure is not fearful, but important thing is how to face it correctly.""",
        "A man may usually be known by the books he reads as well as by the company he keeps; for there is a companionship of books as well as of men; and one should always live in the best company, whether it be of books or of men.",
        "We are not born with courage, but neither are we born with fear. Maybe some of our fears are brought on by your own experiences, by what someone has told you, by what you’ve read in the papers.",
        "Lost time is never found again. This is something which I learned very clearly last semester. I spent so much time fooling around that my grades began to suffer. I finally realized that something had to be done."
    )

    fun getChineseSingleList(): ArrayList<String> {
        val wordLength = chineseWordList.size
        val hashSet = HashSet<String>()
        while(hashSet.size < 10){
            val index = (0 until wordLength).random()
            val word = chineseWordList[index]
            hashSet.add(word)
        }
        return ArrayList(hashSet)
    }

    fun getChineseWordsList(): ArrayList<String> {
        val wordLength = chineseWordsList.size
        val hashSet = HashSet<String>()
        while(hashSet.size < 10){
            val index = (0 until wordLength).random()
            val word = chineseWordsList[index]
            hashSet.add(word)
        }
        return ArrayList(hashSet)
    }

    fun getChineseSentence(): String? {
        val wordLength = chineseSentenceList.size
        val index = (0 until wordLength).random()
        if (index in 0 until wordLength) {
            return chineseSentenceList[index]
        }
        return null
    }

    fun getEnglishSingleWordList(): ArrayList<String> {
        val wordLength = englishWordList.size
        val hashSet = HashSet<String>()
        while(hashSet.size < 10){
            val index = (0 until wordLength).random()
            val word = englishWordList[index]
            hashSet.add(word)
        }
        return ArrayList(hashSet)
    }

    fun getEnglishSentence(): String? {
        val wordLength = englishSentenceList.size
        val index = (0 until wordLength).random()
        if (index in 0 until wordLength) {
            return englishSentenceList[index]
        }
        return null
    }

    fun getEnglishArticle(): String? {
        val wordLength = englishArticleList.size
        val index = (0 until wordLength).random()
        if (index in 0 until wordLength) {
            return englishArticleList[index]
        }
        return null
    }

    fun getPunctuationList(text: String): ArrayList<String> {
        val list = ArrayList<String>()
        for (str in text) {
            val value = str.toString()
            if (TextUtils.equals(",", value) ||
                TextUtils.equals(".", value) ||
                TextUtils.equals("，", value) ||
                TextUtils.equals("。", value) ||
                TextUtils.equals("?", value) ||
                TextUtils.equals(";", value)
            ) {
                list.add(value)
            }
        }
        return list
    }

    /**
     * 移除英文字母
     */
    fun filterSentence(sentence: String?): String? {
        return sentence?.replace(Regex("[a-zA-Z0-9<>]"), "")
    }
}