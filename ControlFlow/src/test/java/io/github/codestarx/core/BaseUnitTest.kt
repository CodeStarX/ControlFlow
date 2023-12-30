package io.github.codestarx.core


import io.mockk.MockKAnnotations
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import java.lang.reflect.Field

@RunWith(BlockJUnit4ClassRunner::class)
open class BaseUnitTest {

    open fun onSetUpTest() {}

    open fun onStopTest() {}

    @Before
    fun onSetup() {
        MockKAnnotations.init(this)
        onSetUpTest()
    }

    @After
    fun onTearDown() {
        unmockkAll()
        onStopTest()
    }

    protected fun getVariableValue(obj: Any, fieldName: String): Any? {
        val field: Field = try {
            obj.javaClass.getDeclaredField(fieldName)
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
            return null
        }

        field.isAccessible = true
        return field.get(obj)
    }

}
