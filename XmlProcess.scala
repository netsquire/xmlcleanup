
import play.api.libs.json.{JsValue, Json, OFormat}
import scala.util.Using
import java.io.{File, PrintWriter}
import scala.xml.*

object XmlProcess {

  // objects in informatica
  // WORKFLOW >> SESSION -> MAPPING > TRANSFORMATION
  // WORKFLOW folds SESSIONs
  // SESSION has_attribute MAPPINGNAME -> link to MAPPING
  // ((TRANSFORMATION) INSTANCE == TABLE, with invalid attribute 'DBDNAME')
  // (FIELD == COLUMN)
  // MAPPING folds CONNECTORs
  // CONNECTOR has attributes TOFIELD and TOINSTANCE, supposedly meaning COLUMN and TABLE [*]
  // no such thing like SCHEMA or smth
  // L:364 <TRANSFORMFIELD DATATYPE ="nstring" DEFAULTVALUE ="ERROR(&apos;transformation error&apos;)" EXPRESSION ="CONCAT(FirstName, CONCAT(MiddleName,LastName))" EXPRESSIONTYPE ="GENERAL" NAME ="NewFullName" PORTTYPE ="OUTPUT" PRECISION ="10" SCALE ="0"/>


  // so, task - to count unique pairs of TOFIELD and TOINSTANCE in CONNECTORs from valid flow [*]
  // [*] traversal of an arbitrary tree with counting its nodes and attributes - distinct and non distinct

  def main(args: Array[String]): Unit = {
    val filename = "resources/input.xml"
    println("= XML Processing =")
    val xml: Elem = XML.loadFile(filename)
    println("LABEL: " + xml.label)
    //    val DOC_ROOT = "_"
    val FETCH_SOURCE = "SOURCE"
    val FETCH_TARGET = "TARGET"

    //    val folder: Folder = (xml \ "POWERMART" \ "REPOSITORY" \ "FOLDER").map(Folder.fromXml).head
    val folders = (xml \\ "FOLDER")
    val folder: Folder = folders.map(Folder.fromXml).head

    val sessions = xml \\ "WORKFLOW" \\ "SESSION"
    val validMappings: Seq[String] = sessions.filter(isValid).map(s => mappingName(s))
    val tnames: List[String] = (for it <- sessions yield it.toString).toList

    val filteredMappings = (xml \\ "MAPPING").filterNot(m => validMappings.contains(m.attribute("NAME")))
    val mappings = filteredMappings.map(Mapping.fromXml).toList
    val numConnectors = mappings.head.connectors.length + mappings.tail.head.connectors.length
    println("Found connectors: " + numConnectors)

    val connectors: List[Connector] = mappings.flatMap(m => m.connectors)
    println("Also connectors: " + connectors.length)

    val lineage = connectors.map(c => (From(c.fromInstance, c.fromField) -> To(c.toInstance, c.toField))).toMap
    val jsonMap: String = toJsonString(lineage)
    println(jsonMap)

    val lks: Int = lineage.keySet.size
    val ms: Int = lineage.size
    println("Map keys size: " + lks)
    println("Map size: " + ms)
    val json: String = toJsonString(lineage)
    writeToFile(json, "resources/lineage.json")
  }

  private def isValid(node: Node): Boolean = {
    node.attribute("ISVALID").toString.contains("YES")
  }

  private def mappingName(node: Node): String = {
    node.attribute("MAPPINGNAME").get.toString
  }

  private def toJsonString(lineage: Map[From, To]): String = {
    val s1: String = lineage.keySet.map(
      k => {
        val v = lineage(k)
        s" \"${k}\": \"${v}\""
      }
    ).toString
    val jsonList = deparasite(s1)
    s"{ $jsonList }"
  }

  private def deparasite(s: String): String = {
    val parasite: Int = "HASHSET(".length
    s.substring(parasite) // removes "HASHSET(" at the beginning
      .substring(0, s.length - (parasite + 1)) // and ")" at the end
  }

  private def writeToFile(content: String, fileName: String): Unit = {
    Using(new PrintWriter(new File(fileName))) { writer =>
      writer.write(content)
    }.fold(
      error => println(s"Failed to write file: ${error.getMessage}"),
      _ => println(s"Successfully wrote to $fileName")
    )
  }

}










//    val xmlFile = new java.io.File(filename)
//    val parser = scala.xml.parsing.ConstructingParser.fromFile(xmlFile, true)
//    val doc = parser.document()
//    println(doc.docElem)
// LEFT: for case if DTD in provided
//    val f = javax.xml.parsers.SAXParserFactory.newInstance()
//    f.setValidating(false)
//    val p = f.newSAXParser()
//    val doc = XML.withSAXParser(p).load(filename)