package views

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.droidhen.formalautosim.presentation.theme.unable_views

@Composable
fun FASButton(
    text:String,
    enabled:Boolean = true,
    modifier:Modifier = Modifier,
    height:Int = 48,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(targetValue = if (isPressed) 0.2f else 1f, label = "button animation",)

    Button(
        onClick = {
            if(enabled){
                isPressed = true
                onClick()
                isPressed = false
            }
        },
        modifier = modifier
            .padding(2.dp)
            .width(110.dp)
            .height(height.dp)
            .scale(scale)
            .border(3.dp,shape = RoundedCornerShape(10.dp),color = if(enabled) MaterialTheme.colorScheme.secondary else unable_views)
            .shadow(3.dp, RoundedCornerShape(10.dp), false, MaterialTheme.colorScheme.secondary)
            ,
        shape = RoundedCornerShape(10.dp),
        colors = if(enabled)ButtonDefaults.buttonColors() else ButtonDefaults.buttonColors(
            unable_views),
    ) {
        Text(text = text, color = MaterialTheme.colorScheme.surface)
    }
}