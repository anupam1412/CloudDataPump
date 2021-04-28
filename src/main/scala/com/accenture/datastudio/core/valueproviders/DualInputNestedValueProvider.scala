package com.accenture.datastudio.core.valueproviders


import com.google.common.base.MoreObjects
import org.apache.beam.sdk.options.ValueProvider
import org.apache.beam.sdk.transforms.SerializableFunction

case class TranslatorInput[FirstT,SecondT](x: FirstT, y: SecondT)

class DualInputNestedValueProvider[T,FirstT,SecondT](valueX:ValueProvider[FirstT],
                                                     valueY:ValueProvider[SecondT],
                                                     translator:SerializableFunction[TranslatorInput[FirstT, SecondT], T]
                                                    )
  extends ValueProvider[T] with Serializable {

  @transient @volatile  var cachedValue: T = _

  override def get(): T = {
    if(cachedValue == null){
      cachedValue = translator.apply(TranslatorInput(valueX.get(), valueY.get()))
    }
    cachedValue
  }

  override def isAccessible: Boolean = valueY.isAccessible && valueY.isAccessible

  override def toString: String = {
    if(isAccessible) String.valueOf(get())
    MoreObjects.toStringHelper(this)
      .add("valueX", valueX)
      .add("valueY", valueY)
      .add("translator", translator.getClass.getSimpleName)
      .toString
  }
}
object DualInputNestedValueProvider {
  def of[T, FirstT, SecondT](valueX:ValueProvider[FirstT],
                             valueY: ValueProvider[SecondT],
                             translator:SerializableFunction[TranslatorInput[FirstT, SecondT], T]
                            ):DualInputNestedValueProvider[T, FirstT,SecondT] = {

    new DualInputNestedValueProvider[T, FirstT, SecondT](valueX, valueY, translator)

  }
}
