package com.fwdekker.randomness.datetime

import com.fwdekker.randomness.Bundle
import com.fwdekker.randomness.SchemeEditor
import com.fwdekker.randomness.array.ArrayDecoratorEditor
import com.fwdekker.randomness.datetime.DateTimeScheme.Companion.DEFAULT_MAX_DATE_TIME
import com.fwdekker.randomness.datetime.DateTimeScheme.Companion.DEFAULT_MIN_DATE_TIME
import com.fwdekker.randomness.ui.JDateTimeField
import com.fwdekker.randomness.ui.UIConstants
import com.fwdekker.randomness.ui.bindDateTimes
import com.fwdekker.randomness.ui.bindTimestamp
import com.fwdekker.randomness.ui.withFixedWidth
import com.fwdekker.randomness.ui.withName
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel


/**
 * Component for editing a [DateTimeScheme].
 *
 * @param scheme the scheme to edit
 */
class DateTimeSchemeEditor(scheme: DateTimeScheme = DateTimeScheme()) : SchemeEditor<DateTimeScheme>(scheme) {
    override val rootComponent = panel {
        group(Bundle("datetime.ui.value.header")) {
            lateinit var minDateTimeField: JDateTimeField
            lateinit var maxDateTimeField: JDateTimeField

            row(Bundle("datetime.ui.value.min_datetime_option")) {
                cell(JDateTimeField(DEFAULT_MIN_DATE_TIME))
                    .withFixedWidth(UIConstants.SIZE_LARGE)
                    .withName("minDateTime")
                    .bindTimestamp(scheme::minDateTime)
                    .bindValidation(scheme::minDateTime)
                    .also { minDateTimeField = it.component }
            }

            row(Bundle("datetime.ui.value.max_datetime_option")) {
                cell(JDateTimeField(DEFAULT_MAX_DATE_TIME))
                    .withFixedWidth(UIConstants.SIZE_LARGE)
                    .withName("maxDateTime")
                    .bindTimestamp(scheme::maxDateTime)
                    .bindValidation(scheme::maxDateTime)
                    .also { maxDateTimeField = it.component }
            }.bottomGap(BottomGap.SMALL)

            bindDateTimes(minDateTimeField, maxDateTimeField)

            row(Bundle("datetime.ui.value.pattern_option")) {
                textField()
                    .withFixedWidth(UIConstants.SIZE_VERY_LARGE)
                    .comment(Bundle("datetime.ui.pattern_comment"))
                    .withName("pattern")
                    .bindText(scheme::pattern)
                    .bindValidation(scheme::pattern)

                browserLink(Bundle("datetime.ui.value.pattern_help"), Bundle("datetime.ui.value.pattern_help_url"))
            }
        }

        row {
            ArrayDecoratorEditor(scheme.arrayDecorator)
                .also { decoratorEditors += it }
                .let { cell(it.rootComponent).align(AlignX.FILL) }
        }
    }.finalize(this)


    init {
        reset()
    }
}
