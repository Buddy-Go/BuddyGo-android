package com.example.myapplication.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

// 로그인 토큰이 든 설정 파일이 기기 백업·이전에 실려 가지 않는지 지킨다.
class SessionBackupRulesTest {
    private val file = "$SESSION_PREFS_NAME.xml"

    private fun excludes(xml: String, parentTag: String?): List<Pair<String, String>> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File("src/main/res/xml/$xml"))
        val scope = if (parentTag == null) doc.documentElement else doc.getElementsByTagName(parentTag).item(0) as org.w3c.dom.Element
        val nodes = scope.getElementsByTagName("exclude")
        return (0 until nodes.length).map { i ->
            val e = nodes.item(i) as org.w3c.dom.Element
            e.getAttribute("domain") to e.getAttribute("path")
        }
    }

    @Test
    fun 안드로이드_11_이하_백업에서_토큰_파일을_뺀다() {
        assertTrue(("sharedpref" to file) in excludes("backup_rules.xml", null))
    }

    @Test
    fun 클라우드_백업에서_토큰_파일을_뺀다() {
        assertTrue(("sharedpref" to file) in excludes("data_extraction_rules.xml", "cloud-backup"))
    }

    @Test
    fun 기기_간_전송에서도_토큰_파일을_뺀다() {
        assertTrue(("sharedpref" to file) in excludes("data_extraction_rules.xml", "device-transfer"))
    }

    @Test
    fun 설정_파일_이름은_토큰_저장소가_쓰는_이름과_같다() {
        assertEquals("buddygo_session", SESSION_PREFS_NAME)
    }
}
