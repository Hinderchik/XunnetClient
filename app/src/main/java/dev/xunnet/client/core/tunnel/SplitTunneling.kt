package dev.xunnet.client.core.tunnel

/**
 * Curated IP CIDR lists for split tunneling.
 * - Russian services: routes DIRECT to avoid latency and preserve access to local services
 * - Streaming (Netflix/AKAMAI/Cloudflare): routes DIRECT for native quality
 * - Gaming: routes DIRECT for low latency
 *
 * Sources: public RIR delegations, APNIC, RIPE, plus hand-curated ranges for major CDNs.
 */
object SplitTunneling {

    /** Russian internet — banks, government, services, popular CDNs. */
    val russia: List<String> = listOf(
        // Yandex
        "77.88.55.0/24", "77.88.55.50/24", "5.45.205.0/24", "5.255.255.0/24",
        "141.8.142.0/24", "87.250.250.0/24", "130.193.32.0/19",
        // VK
        "87.240.128.0/18", "93.186.224.0/22", "95.213.0.0/16", "104.244.0.0/16",
        "185.32.248.0/22", "185.32.252.0/22",
        // Mail.ru
        "94.100.176.0/20", "217.69.128.0/20", "128.140.168.0/21",
        // Sberbank / Tinkoff / banks
        "194.54.14.0/23", "194.186.0.0/16", "195.122.0.0/16", "213.59.0.0/16",
        // Government / GosUslugi
        "78.108.192.0/20", "85.143.0.0/16", "87.244.0.0/16",
        // Russian social media / messengers (max, ok, vk, mail, yandex)
        "178.22.192.0/19", "185.137.182.0/24", "188.168.0.0/16",
        // RKN / telecom
        "109.252.0.0/16", "178.34.0.0/15", "85.140.0.0/12",
        // Telegram DCs (Russia)
        "91.108.4.0/22", "91.108.8.0/22", "91.108.12.0/22", "91.108.16.0/22",
        "91.108.20.0/22", "91.108.56.0/22", "149.154.160.0/20",
        // WhatsApp/Meta (Russia is partial)
        "157.240.0.0/16", "31.13.0.0/16",
        // DNS Russia
        "77.88.8.8/32", "77.88.8.1/32", "77.88.8.2/32"
    )

    /** Streaming CDNs — bypass for native quality. */
    val streaming: List<String> = listOf(
        // Netflix
        "45.57.0.0/16", "198.38.96.0/19", "198.38.100.0/22", "198.38.108.0/22",
        "185.2.220.0/22", "185.9.188.0/22", "192.173.0.0/16", "207.45.72.0/22",
        "208.75.76.0/22", "23.246.0.0/18", "37.77.184.0/21", "64.120.128.0/17",
        "66.197.128.0/17", "69.53.224.0/19", "108.175.32.0/20",
        // YouTube / Google
        "172.217.0.0/16", "216.58.192.0/19", "74.125.0.0/16", "142.250.0.0/15",
        "173.194.0.0/16", "209.85.128.0/17", "108.177.0.0/17",
        // Akamai (used by many CDNs)
        "23.32.0.0/11", "23.192.0.0/11", "23.0.0.0/12", "104.64.0.0/10",
        "184.24.0.0/13", "184.50.96.0/20", "2.16.0.0/13", "88.221.0.0/16",
        // Cloudflare
        "173.245.48.0/20", "103.21.244.0/22", "103.22.200.0/22", "103.31.4.0/22",
        "141.101.64.0/18", "108.162.192.0/18", "190.93.240.0/20", "188.114.96.0/20",
        "197.234.240.0/22", "198.41.128.0/17", "162.158.0.0/15", "104.16.0.0/12",
        "172.64.0.0/13", "131.0.72.0/22"
    )

    /** Game servers — bypass for low latency. */
    val gaming: List<String> = listOf(
        // Steam
        "155.133.224.0/19", "162.254.192.0/21", "185.25.180.0/22",
        "192.69.96.0/22", "205.196.6.0/23", "208.64.200.0/22",
        // Riot Games / LoL / Valorant
        "104.160.128.0/17", "162.249.72.0/22", "45.7.36.0/22", "45.7.37.0/24",
        "162.249.76.0/22", "162.249.78.0/23", "64.7.192.0/18",
        // Epic Games / Fortnite
        "34.192.0.0/14", "34.196.0.0/14", "34.200.0.0/14", "35.168.0.0/13",
        "52.0.0.0/11", "52.32.0.0/12", "52.48.0.0/13", "52.64.0.0/11",
        "52.84.0.0/15", "52.200.0.0/13", "54.64.0.0/11", "54.144.0.0/12",
        "54.192.0.0/12", "54.208.0.0/13", "54.224.0.0/12",
        // Blizzard / Battle.net
        "12.129.0.0/16", "24.105.0.0/17", "24.105.128.0/17", "24.32.0.0/14",
        "63.222.0.0/16", "63.228.0.0/16", "64.7.192.0/18", "67.23.0.0/16",
        // Discord voice
        "162.159.128.0/17", "162.159.0.0/16"
    )

    /** All bypass rules combined. */
    val all: List<String> = (russia + streaming + gaming).distinct()

    /**
     * Get the right list for a preset name.
     * - "default" / "proxy"  → empty (everything via tunnel)
     * - "russia"             → russia list
     * - "streaming"          → streaming list
     * - "gaming"             → gaming list
     * - "full"               → all
     */
    fun forPreset(preset: String): List<String> = when (preset.lowercase()) {
        "russia", "ru" -> russia
        "streaming", "media", "netflix" -> streaming
        "gaming", "games" -> gaming
        "full", "all" -> all
        else -> emptyList()
    }
}
