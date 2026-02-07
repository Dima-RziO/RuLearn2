package ru.dimarzio.rulearn2.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.VisualTransformation
import ru.dimarzio.rulearn2.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange = Int.MIN_VALUE..Int.MAX_VALUE,
    label: String = ""
) {
    TextFieldDefaults.DecorationBox(
        value = value.toString(),
        innerTextField = {
            Text(text = value.toString())
        },
        enabled = true,
        singleLine = false,
        visualTransformation = VisualTransformation.None,
        interactionSource = remember { MutableInteractionSource() },
        label = {
            Text(label)
        },
        trailingIcon = {
            Column {
                Icon(
                    painter = painterResource(R.drawable.baseline_arrow_up_24),
                    contentDescription = "+",
                    modifier = Modifier.clickable {
                        if (value + 1 in range) {
                            onValueChange(value + 1)
                        }
                    }
                )

                Icon(
                    painter = painterResource(R.drawable.baseline_arrow_down_24),
                    contentDescription = "-",
                    modifier = Modifier.clickable {
                        if (value - 1 in range) {
                            onValueChange(value - 1)
                        }
                    }
                )
            }
        }
    )
}