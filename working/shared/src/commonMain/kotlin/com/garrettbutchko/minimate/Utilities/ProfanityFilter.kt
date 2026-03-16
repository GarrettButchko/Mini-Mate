package com.garrettbutchko.minimate.utilities

object ProfanityFilter {
    private val baseWords = setOf(
        "anal", "arse", "ass", "bastard", "bawdy", "bitch", "blow", "bollock", "boner", "boob",
        "booty", "bugger", "bullshit", "butt", "cameltoe", "carpet", "chink", "choad", "clit", "cluster",
        "cock", "coon", "cooch", "cooter", "crap", "cum", "cunt", "damn", "degenerate", "dick",
        "dildo", "dipshit", "douche", "dyke", "fag", "fanny", "feltch", "fingerbang", "fist", "flamer",
        "freak", "fuck", "gangbang", "gay", "goatse", "goddamn", "gook", "grope", "handjob", "hardcore",
        "hoe", "homo", "honkey", "horny", "hump", "jack off", "jap", "jerk", "jigaboo", "jizz",
        "kike", "kkk", "kunt", "labia", "lesbo", "lezzie", "lust", "masturb", "milf", "minge",
        "molest", "motherfuck", "muff", "mung", "nazi", "negro", "nig", "nutsack", "orgasm", "paki",
        "panty", "pecker", "penis", "pervert", "phag", "piss", "poon", "porn", "prick", "pube",
        "puss", "queef", "queer", "quim", "rape", "raunch", "rectum", "retard", "rim", "sack",
        "sadist", "schlong", "scrote", "scum", "semen", "sex", "shag", "shit", "skank", "skeet",
        "slut", "smeg", "smut", "snatch", "spaz", "spic", "spooge", "suck", "suckmy", "swallow",
        "teabag", "testicle", "tit", "toke", "tosser", "turd", "twat", "vag", "vagina", "vibe",
        "vibrator", "vulva", "wank", "wetback", "whore", "womb", "wop", "xxx", "yaoi", "yiffy",
        "zoophile", "zoosex", "abuse", "addict", "anus", "bang", "beaver", "beefcurtain", "bimbo", "blowjob",
        "bodily", "boink", "bonk", "bootie", "brutal", "bukake", "bull", "camel", "cheeks", "climax", "cock",
        "cocky", "condom", "corrupt", "crotch", "crude", "cummer", "cunning", "curs", "cutter", "dammit",
        "deepthroat", "degrade", "deviant", "diaper", "dirtypillows", "dominatrix", "dragqueen", "ejacul", "enema", "erect",
        "escort", "excrement", "expose", "extort", "fetish", "filth", "flesh", "fondl", "fornic", "g-spot",
        "genital", "groin", "gypsy", "hardon", "hentai", "hooker", "horn", "hustler", "incest", "innuendo",
        "intercourse", "jiggle", "kama", "kinky", "kissing", "latex", "lecher", "lick", "lingerie", "loins",
        "lube", "lusty", "malestimulation", "manhood", "masochist", "meatstick", "menstru", "miniskirt",
        "missionary", "moan", "molest", "mooch", "moron", "nasty", "nipple", "nookie", "nudity", "obscene", "orifice", "orgy",
        "panties", "pantyhose", "peep", "penetrat", "phallus", "playboy", "pole", "porn", "prostitut",
        "pubic", "puke", "pummel", "raunchy", "rear", "rectal", "rubber", "scat", "seduce", "sensual",
        "sewage", "shame", "shlong", "shove", "shower", "shut", "silly", "slapper", "sleaze", "slink",
        "slope", "smack", "snort", "sodom", "softcore", "spank", "spasm", "spit", "splooge", "spreader",
        "squeeze", "stiff", "strip", "stroke", "stud", "submissive", "sultry", "swine", "syringe", "taint",
        "tease", "tempt", "thong", "tight", "titty", "topless", "tramp", "transvestite", "trollop", "tush",
        "twerk", "undress", "urinal", "urinate", "urinat", "vibrator", "virgin", "voyeur", "wank", "wax",
        "wedge", "wet", "whip", "whitetrash", "wild", "xrate", "yank", "yeast", "zipper", "zits",
        "abortion", "affair", "arse", "asphyx", "babes", "backdoor", "ballgag", "bareback", "bazoom", "bdsm",
        "beast", "beej", "biatch", "bimbo", "bind", "bisex", "blackface", "blow", "bod", "boink",
        "bondage", "bootlick", "bottom", "bra", "breast", "brothel", "bulge", "cage", "carne", "chain",
        "cheek", "clench", "climax", "cling", "clown", "cockpit", "come", "consent", "contracept", "cuddle",
        "cunniling", "defile", "desire", "devour", "dirty", "discharge", "dominate", "donkey", "drench",
        "drip", "cumshot", "creamp", "breed", "nut", "bust", "facial", "thrust", "stroke", "shaft", "spread",
        "bare", "load", "seed",
        "assplay", "analplay", "oral",
        "balls", "ballz", "ballsack",
        "girth", "head", "rimjob",
        "kink", "domme", "sub", "subby",
        "daddy", "master", "slave",
        "collar", "leash", "petplay",
        "humiliate", "discipline",
        "dtf", "hookup", "fwb",
        "quickie", "sugar",
        "cashapp", "venmo", "paypal",
        "rates", "services",
        "fgt", "tranny", "raghead",
        "whitepower", "heil",
        "nazi", "hitler",
        "ejac", "orgasm", "penetrat",
        "fornic", "masturb", "sodom",
        "molest", "rape"
    )

    private val substitutions = mapOf(
        'a' to "a4@",
        'b' to "b8",
        'c' to "c(",
        'e' to "e3",
        'i' to "i1!",
        'l' to "l1|",
        'o' to "o0",
        's' to "s5$",
        't' to "t7+",
        'g' to "g9"
    )

    private fun getPattern(word: String): String {
        return word.map { char ->
            val chars = substitutions[char.lowercaseChar()] ?: char.toString()
            "[$chars]"
        }.joinToString("")
    }

    private val regex: Regex by lazy {
        val regexPattern = baseWords
            .joinToString("|") { getPattern(it) }
        Regex(regexPattern, RegexOption.IGNORE_CASE)
    }

    fun containsBlockedWord(text: String): Boolean {
        return regex.containsMatchIn(text)
    }
}
