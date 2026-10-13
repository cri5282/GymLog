package it.gymlog.app.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun muscleToString(v: Muscle): String = v.name
    @TypeConverter fun stringToMuscle(v: String): Muscle = Muscle.valueOf(v)
    @TypeConverter fun biasToString(v: LengthBias): String = v.name
    @TypeConverter fun stringToBias(v: String): LengthBias = LengthBias.valueOf(v)
    @TypeConverter fun setTypeToString(v: SetType): String = v.name
    @TypeConverter fun stringToSetType(v: String): SetType = SetType.valueOf(v)
    @TypeConverter fun sourceToString(v: SessionSource): String = v.name
    @TypeConverter fun stringToSource(v: String): SessionSource = SessionSource.valueOf(v)
}
