//package com.johnson.sketchclock.template_editor
//
//import com.johnson.sketchclock.common.Template
//import com.johnson.sketchclock.common.TemplateElement
//import com.johnson.sketchclock.common.ElementType
//import java.io.Serializable
//
//fun Template.toMutableTemplate(): MutableTemplate {
//    return MutableTemplate(id, name, fontId, elements)
//}
//
//fun MutableTemplate.toTemplate(): Template {
//    return Template(id, name, fontId, elements)
//}
//
//fun TemplateElement.toMutableElement(): MutableElement {
//    return MutableElement(elementType, x, y, scale, rotation)
//}
//
//fun MutableElement.toElement(): TemplateElement {
//    return TemplateElement(elementType, x, y, scale, rotation)
//}
//
//class MutableTemplate(
//    id: Int?,
//    name: String,
//    fontId: Int,
//    elements: List<TemplateElement>
//) : Template (
//    id,
//    name,
//    -1,
//    emptyList()
//) {
//
//    private val mutableElements = elements.toMutableList()
//    private var mutableFontId = fontId
//
//    override val elements: List<TemplateElement>
//        get() = mutableElements
//
//    override val fontId: Int
//        get() = mutableFontId
//
//    fun setElements(elements: List<TemplateElement>) {
//        mutableElements.clear()
//        mutableElements.addAll(elements)
//    }
//
//    fun setFontId(fontId: Int) {
//        mutableFontId = fontId
//    }
//}
//
//class MutableElement(
//    elementType: ElementType,
//    x: Float,
//    y: Float,
//    scale: Float,
//    rotation: Float
//) : TemplateElement(
//    elementType,
//    x,
//    y,
//    scale,
//    rotation
//) {
//
//    private var mutableX = x
//    private var mutableY = y
//    private var mutableScale = scale
//    private var mutableRotation = rotation
//
//    override val x: Float
//        get() = mutableX
//
//    override val y: Float
//        get() = mutableY
//
//    override val scale: Float
//        get() = mutableScale
//
//    override val rotation: Float
//        get() = mutableRotation
//
//    fun setX(x: Float) {
//        mutableX = x
//    }
//
//    fun setY(y: Float) {
//        mutableY = y
//    }
//
//    fun setScale(scale: Float) {
//        mutableScale = scale
//    }
//
//    fun setRotation(rotation: Float) {
//        mutableRotation = rotation
//    }
//}