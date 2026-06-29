package dev.xunnet.client.core.domain.parser

import dev.xunnet.client.core.domain.model.Profile
import java.io.File

interface LinkParser {
    fun parse(link: String): Result<Profile>
    fun parseFile(file: File): Result<List<Profile>>
    /**
     * Parse a WireGuard / AmneziaWG configuration file (.conf / .wgconf) with
     * the standard [Interface] + [Peer] sections and AWG obfuscation params.
     */
    fun parseConfig(conf: String): Result<Profile>
    fun generate(profile: Profile): String
    fun generateXunCrypt(profile: Profile, password: String): String
}
