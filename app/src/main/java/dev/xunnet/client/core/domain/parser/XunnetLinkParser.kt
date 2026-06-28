package dev.xunnet.client.core.domain.parser

import dev.xunnet.client.core.domain.model.Profile
import java.io.File

class XunnetLinkParser : LinkParser {
    override fun parse(link: String): Result<Profile> = Result.failure(NotImplementedError())
    override fun parseFile(file: File): Result<List<Profile>> = Result.failure(NotImplementedError())
    override fun generate(profile: Profile): String = ""
    override fun generateXunCrypt(profile: Profile, password: String): String = ""
}
