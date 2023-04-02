package hotkitchen.utils

val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$"
fun checkEmail(email: String): Boolean = email.matches(emailRegex.toRegex())

fun checkPassword(password: String): Boolean = password.length < 6
        || password.all { char -> char.isDigit() }
        || password.all { char ->  char.isLetter() }
        || password.all { char -> !char.isLetter() && !char.isDigit()}