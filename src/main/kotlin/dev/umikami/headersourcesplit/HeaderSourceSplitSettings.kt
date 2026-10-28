// Copyright 2026 Ernesto Gonzalez. Licensed under the Apache License, Version 2.0.
// See the LICENSE and NOTICE files in the project root.

package dev.umikami.headersourcesplit

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil
import javax.swing.JSplitPane

enum class SplitLayout(val splitOrientation: Int) {
    /** Panes next to each other: left | right. */
    SIDE_BY_SIDE(JSplitPane.HORIZONTAL_SPLIT),

    /** Panes on top of each other: top / bottom. */
    STACKED(JSplitPane.VERTICAL_SPLIT),
}

@Service(Service.Level.APP)
@State(name = "HeaderSourceSplitSettings", storages = [Storage("headerSourceSplit.xml")])
class HeaderSourceSplitSettings : PersistentStateComponent<HeaderSourceSplitSettings.Data> {
    class Data {
        var enabled: Boolean = true
        var layout: SplitLayout = SplitLayout.SIDE_BY_SIDE

        /** true: header left/top, source right/bottom. false: the reverse. */
        var headerFirst: Boolean = true
    }

    private val data = Data()

    override fun getState(): Data = data

    override fun loadState(state: Data) {
        XmlSerializerUtil.copyBean(state, data)
    }

    companion object {
        fun getInstance(): HeaderSourceSplitSettings = service()
    }
}
