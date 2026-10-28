// Copyright 2026 Ernesto Gonzalez. Licensed under the Apache License, Version 2.0.
// See the LICENSE and NOTICE files in the project root.

package dev.umikami.headersourcesplit

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel

class HeaderSourceSplitConfigurable : BoundConfigurable("Header/Source Split") {
    private val data get() = HeaderSourceSplitSettings.getInstance().state

    override fun createPanel(): DialogPanel = panel {
        row {
            checkBox("Open the matching C/C++ header or source file in a split")
                .bindSelected(data::enabled)
        }
        buttonsGroup("Layout:") {
            row { radioButton("Side by side (left | right)", SplitLayout.SIDE_BY_SIDE) }
            row { radioButton("Stacked (top / bottom)", SplitLayout.STACKED) }
        }.bind(data::layout)
        buttonsGroup("Order:") {
            row { radioButton("Header left/top, source right/bottom", true) }
            row { radioButton("Source left/top, header right/bottom", false) }
        }.bind(data::headerFirst)
    }
}
