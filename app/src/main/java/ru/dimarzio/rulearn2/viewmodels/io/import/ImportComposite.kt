package ru.dimarzio.rulearn2.viewmodels.io.import

open class ImportComposite(name: String) : ImportComponent(name) {
    private val _children = mutableListOf<ImportComponent>()

    val children: List<ImportComponent> get() = _children

    override fun add(component: ImportComponent) {
        _children.add(component)
    }

    override fun remove(component: ImportComponent) {
        _children.remove(component)
    }

    override fun getProgress(): Float {
        return _children.map(ImportComponent::getProgress).average().toFloat()
    }
}