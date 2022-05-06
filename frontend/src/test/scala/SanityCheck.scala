import org.scalatest.funspec.AnyFunSpec
import com.raquo.laminar.api.L._
import org.scalajs.dom
import org.scalatest.matchers.should.Matchers

class CardTest extends AnyFunSpec with Matchers {

  it("should run a test with the dom enabled") {

    val nameVar   = Var("This name")
    val component = div(onMountCallback(ctx => nameVar.set("Another Name")))
    render(dom.document.body, component)

    nameVar.now() shouldEqual "Another Name"
    nameVar.now() shouldNot be("This Name")
  }

}
