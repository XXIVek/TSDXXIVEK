package com.xxivek.tsdxxivek.dataXML

import android.util.Log
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.IOException
import java.io.InputStream
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

class XMLDOMParser {
    //Возвращает весь XML-документ целиком
    fun getDocument(inputStream: InputStream?): Document? {
        val document: Document
        val factory = DocumentBuilderFactory.newInstance()
        document = try {
            val db: DocumentBuilder = factory.newDocumentBuilder()
            val inputSource = InputSource(inputStream)
            db.parse(inputSource)
        } catch (e: ParserConfigurationException) {
            Log.e("Error: ", e.message.toString())
            return null
        } catch (e: SAXException) {
            Log.e("Error: ", e.message.toString())
            return null
        } catch (e: IOException) {
            Log.e("Error: ", e.message.toString())
            return null
        }
        return document
    }

    /*
     Я беру XML-элемент и имя тега, ищу тег и получаю
        текстовое содержимое, например, для <сотрудника><имя>Кумар</имя></сотрудник>
        Фрагмент XML, если элемент указывает на узел сотрудника и имя тега
        это имя я верну Кумару. Вызывает частный метод
        getTextNodeValue(node), который возвращает текстовое значение, скажем, в нашем
        пример Кумара.
     */
    fun getValue(item: Element, name: String?): String {
        val nodes: NodeList = item.getElementsByTagName(name)
        return getTextNodeValue(nodes.item(0))
    }

    private fun getTextNodeValue(node: Node?): String {
        var child: Node
        if (node != null) {
            if (node.hasChildNodes()) {
                child = node.getFirstChild()
                while (child != null) {
                    if (child.getNodeType() === Node.TEXT_NODE) {
                        return child.getNodeValue()
                    }
                    child = child.getNextSibling()
                }
            }
        }
        return ""
    }
}