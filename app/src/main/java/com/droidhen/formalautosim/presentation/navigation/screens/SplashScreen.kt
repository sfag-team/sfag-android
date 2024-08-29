package com.droidhen.formalautosim.presentation.navigation.screens

import androidx.compose.runtime.Composable

@Composable
fun SplashScreen(navigateToNextScreen:()->Unit, navigateToMainActivity:()->Unit){
    navigateToNextScreen()
    /*TODO - тут вью-модель должна решать отправляемся ли на сайн-ин или сразу на симулятор.
        Сплэш отрабатывает 800 миллисекунд, и будет работать дальше если вью-модель нужно время чтобы подгрузиться.
         Проверка на интернет, таймаут 6 секунд - оффлайн мод*/
}