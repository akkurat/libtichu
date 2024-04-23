package ch.taburett.tichu.cards

fun main(args: Array<String>) {
    println("Hello World!")

    // Try adding program arguments via Run/Debug configuration.
    // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
    println("Program arguments: ${args.joinToString()}")

    val list =listOf(1,2,3,4,5,6)
    for( i in list.indices  )
    {
        println(i)
    }

    for( i in list.indices - 1 )
    {
        println(i)
    }
}