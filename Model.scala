
import play.api.libs.json.{Json, OFormat}
import scala.xml.Node

// to consider: generic functions
// to consider: all strings -> Enums

extension (x: Node){
def getAttribute(attr: String): String = {
  x.attribute(attr)
    .getOrElse(List(""))
    .head
    .toString
}}

class Point(table: String, column: String)
case class From(table: String, column: String) extends Point(table: String, column: String) {
  override def toString: String = simplifyJson(table, column) // to Point(From)
}
case class To(table: String, column: String) extends Point(table: String, column: String){
  override def toString: String = simplifyJson(table, column) // to Point(To)
}

def simplifyJson(table: String, column: String): String =
  table + "_@_" + column // to Point

given toFormat: OFormat[To] = Json.format[To]
given fromFormat: OFormat[From] = Json.format[From]

case class Link(map: Map[Point, Point])

case class TransformField(datatype: String, name: String)

private object TransformField {
  def fromXml(node: Node): TransformField = {
    TransformField(
      datatype = node.getAttribute("DATATYPE"),
      name = node.getAttribute("NAME"))
  }
}

case class Transformation(name: String, transformFields: List[TransformField])

private object Transformation {
  def fromXml(node: Node): Transformation = {
    val transformFields: List[TransformField] = (node \ "TRANSFORMFIELD").map(TransformField.fromXml).toList
    Transformation(
      name = node.getAttribute("NAME"),
      transformFields = transformFields
    )
  }
}

case class Mapping(name: String, isValid: String, transformations: List[Transformation], connectors: List[Connector], instances: List[Instance])

private object Mapping {
  def fromXml(node: Node): Mapping = {
    val transformations: List[Transformation] = (node \ "TRANSFORMATION").map(Transformation.fromXml).toList
    val connectors: List[Connector] = (node \ "CONNECTOR").map(Connector.fromXml).toList
    val instances: List[Instance] = (node \ "INSTANCE").map(Instance.fromXml).toList
    Mapping(
      name = node.getAttribute("NAME"),
      isValid = node.getAttribute("ISVALID"),
      transformations = transformations,
      connectors = connectors,
      instances = instances
    )
  }
}

case class Connector(fromField: String, fromInstance: String, toField: String, toInstance: String)

private object Connector {
  def fromXml(node: Node): Connector =
    Connector(
      fromField = node.getAttribute("FROMFIELD"),
      fromInstance = node.getAttribute("FROMINSTANCE"),
      toField = node.getAttribute("TOFIELD"),
      toInstance = node.getAttribute("TOINSTANCE")
    )
}

case class Instance(name: String, transformationName: String, transformationType: String, instanceType: String)

private object Instance {
  def fromXml(node: Node): Instance =
    Instance(
      name = node.getAttribute("NAME"),
      transformationName = node.getAttribute("TRANSFORMATION_NAME"),
      transformationType = node.getAttribute("TRANSFORMATION_TYPE"),
      instanceType = node.getAttribute("TYPE").toString
    )
}

case class TargetField(dataType: String, keyType: String, name: String)

private object TargetField {
  def fromXml(node: Node): TargetField =
    TargetField(
      name = node.getAttribute("NAME"),
      dataType = node.getAttribute("DATATYPE"),
      keyType = node.getAttribute("KEYTYPE"),
    )
}

case class Target(constraint: String, databaseType: String, name: String, dbdname: String, ownerName: String, targetFields: List[TargetField])

private object Target {
  def fromXml(node: Node): Target = {
    val targetFields: List[TargetField] = (node \ "TARGETFIELD").map(TargetField.fromXml).toList
    Target(
      name = node.getAttribute("NAME"),
      databaseType = node.getAttribute("NAME"),
      constraint = node.getAttribute("CONSTRAINT"),
      dbdname = node.getAttribute("DBDNAME"),
      ownerName = node.getAttribute("OWNERNAME"),
      targetFields = targetFields
    )
  }
}

case class SourceField(name: String, dataType: String, fieldNumber: String, fieldProperty: String, fieldType: String)

private object SourceField {
  def fromXml(node: Node): SourceField =
    SourceField(
      name = node.getAttribute("NAME"),
      dataType = node.getAttribute("DATATYPE"),
      fieldNumber = node.getAttribute("FIELDNUMBER"),
      fieldProperty = node.getAttribute("FIELDPROPERTY"),
      fieldType = node.getAttribute("FIELDTYPE"),
    )
}

case class Source(databaseType: String, dbdName: String, name: String, ownerName: String, sourceFields: List[SourceField])

private object Source {
  def fromXml(node: Node): Source = {
    val sourceFields: List[SourceField] = (node \ "SOURCEFIELD").map(SourceField.fromXml).toList
    Source(
      name = node.getAttribute("NAME"),
      databaseType = node.getAttribute("DATABASETYPE"),
      dbdName = node.getAttribute("DBDNAME"),
      ownerName = node.getAttribute("OWNERNAME"),
      sourceFields = sourceFields
    )
  }
}

case class Folder(name: String, owner: String, mappings: List[Mapping], sources: List[Source], targets: List[Target])

private object Folder {
  def fromXml(node: Node): Folder = {
    val mappings: List[Mapping] = (node \ "MAPPING").map(Mapping.fromXml).toList
    val sources: List[Source] = (node \ "SOURCE").map(Source.fromXml).toList
    val targets: List[Target] = (node \ "TARGET").map(Target.fromXml).toList
    Folder(
      name = node.getAttribute("NAME"),
      owner = node.getAttribute("OWNER"),
      mappings = mappings,
      sources = sources,
      targets = targets
    )
  }
}

