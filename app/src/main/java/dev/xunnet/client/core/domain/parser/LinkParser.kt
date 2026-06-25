package dev.xunnet.client.core.domain.parser

import dev.xunnet.client.core.domain.model.Profile
import java.io.File

interface LinkParser {
    fun parse(link: String): Result<Profile>
    fun parseFile(file: File): Result<List<Profile>>
    fun generate(profile: Profile): String
    fun generateXunCrypt(profile: Profile, password: String): String
}
