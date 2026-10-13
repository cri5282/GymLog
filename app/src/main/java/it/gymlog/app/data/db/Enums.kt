package it.gymlog.app.data.db

enum class Muscle(val label: String) {
    PETTO("Petto"),
    DORSALI("Dorsali"),
    TRAPEZI("Trapezi"),
    DELTOIDE_ANTERIORE("Deltoide anteriore"),
    DELTOIDE_LATERALE("Deltoide laterale"),
    DELTOIDE_POSTERIORE("Deltoide posteriore"),
    BICIPITI("Bicipiti"),
    TRICIPITI("Tricipiti"),
    AVAMBRACCI("Avambracci"),
    ADDOME("Addome"),
    QUADRICIPITI("Quadricipiti"),
    FEMORALI("Femorali"),
    GLUTEI("Glutei"),
    POLPACCI("Polpacci"),
    ADDUTTORI("Adduttori"),
    ABDUTTORI("Abduttori"),
}

enum class LengthBias(val label: String) {
    LENGTHENED("Allungamento"),
    SHORTENED("Accorciamento"),
    NONE("Nessuno"),
}

enum class SetType(val label: String) {
    WARMUP("Riscaldamento"),
    WORKING("Allenante"),
    DROP("Drop"),
    FAILURE("Cedimento"),
}

enum class SessionSource { MANUAL, HEVY_IMPORT }
