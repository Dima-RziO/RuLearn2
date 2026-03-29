package ru.dimarzio.rulearn2.viewmodels.io.export

import java.util.zip.ZipOutputStream

open class ExportComposite(name: String) : ExportComponent(name) { // Composite
    private val _children = mutableListOf<ExportComponent>()

    val children: List<ExportComponent> get() = _children

    override fun export(zos: ZipOutputStream) {
        _children.forEach { child ->
            if (child is Directory) {
                copy(child)
            }
            child.export(zos)
        }
    }

    override fun add(component: ExportComponent) {
        _children.add(component)
    }

    override fun remove(component: ExportComponent) {
        _children.remove(component)
    }

    override fun getProgress(): Float {
        return _children.map(ExportComponent::getProgress).average().toFloat()
    }
}