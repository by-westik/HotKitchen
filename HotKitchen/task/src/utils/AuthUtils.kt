package hotkitchen.utils

class ForbiddenException(message: String?) : Exception(message)
val emailRegex = """(^[a-zA-Z0-9_+-.]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+${'$'})"""
fun checkEmail(email: String) {
    if (!email.matches(emailRegex.toRegex()))
        throw ForbiddenException("Invalid email")
}

fun checkPassword(password: String){
    if (password.length < 6
            || password.all { char -> char.isDigit() }
            || password.all { char ->  char.isLetter() }
            || password.all { char -> !char.isLetter() && !char.isDigit() })
        throw ForbiddenException("Invalid password")
}