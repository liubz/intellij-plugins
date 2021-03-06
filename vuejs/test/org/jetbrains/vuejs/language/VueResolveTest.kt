package org.jetbrains.vuejs.language

import com.intellij.lang.javascript.JSTestUtils
import com.intellij.lang.javascript.psi.JSArrayLiteralExpression
import com.intellij.lang.javascript.psi.JSLiteralExpression
import com.intellij.lang.javascript.psi.JSProperty
import com.intellij.lang.javascript.psi.JSReferenceExpression
import com.intellij.lang.javascript.psi.impl.JSReferenceExpressionImpl
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase
import junit.framework.TestCase
import org.jetbrains.vuejs.codeInsight.VueJSSpecificHandlersFactory

/**
 * @author Irina.Chernushina on 7/28/2017.
 */
class VueResolveTest : LightPlatformCodeInsightFixtureTestCase() {
  fun testResolveInjectionToPropInObject() {
    myFixture.configureByText("ResolveToPropInObject.vue", """
<template>
    <div class="list">
        <ul>
            <li>
                {{ <caret>message25620 }}
            </li>
        </ul>
    </div>
</template>

<script>
  export default {
    name: 'list',
    props: {message25620: {}}
  }
  let message25620 = 111;
</script>""")
    val reference =  myFixture.file.findReferenceAt(myFixture.editor.caretModel.offset)
    TestCase.assertNotNull(reference)
    TestCase.assertTrue(reference is JSReferenceExpression)
    val resolver = VueJSSpecificHandlersFactory().createReferenceExpressionResolver(
      reference as JSReferenceExpressionImpl, true)
    val results = resolver.resolve(reference, false)
    TestCase.assertEquals(1, results.size)
    TestCase.assertTrue(results[0].element!! is JSProperty)
    TestCase.assertEquals("message25620", (results[0].element!! as JSProperty).name)
  }

  fun testResolveUsageInAttributeToPropInArray() {
    myFixture.configureByText("ResolveToPropInObject.vue", """
<template>
  <list25620 v-text="'prefix' + <caret>message25620Arr + 'postfix'">
  Text
  </list25620>
</template>

<script>
  export default {
    name: 'list25620',
    props: ['message25620Arr']
  }
</script>""")
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    TestCase.assertTrue(reference is JSReferenceExpression)
    val resolver = VueJSSpecificHandlersFactory().createReferenceExpressionResolver(
      reference as JSReferenceExpressionImpl, true)
    val results = resolver.resolve(reference, false)
    TestCase.assertEquals(1, results.size)
    val literal = results[0].element!!
    TestCase.assertTrue(literal is JSLiteralExpression)
    TestCase.assertTrue(literal.parent is JSArrayLiteralExpression)
    TestCase.assertEquals("'message25620Arr'", literal.text)
  }

  fun testResolveAttributeInPascalCaseUsageInPropsArray() {
    myFixture.configureByText("ResolveAttributeInPascalCaseUsageInPropsArray.vue", """
<template>
  <list25620 <caret>PascalCase">
  Text
  </list25620>
</template>

<script>
  export default {
    name: 'list25620',
    props: ['pascalCase']
  }
  let message25620 = 111;
</script>""")
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val literal = reference!!.resolve()
    TestCase.assertTrue(literal is JSLiteralExpression)
    TestCase.assertTrue(literal!!.parent is JSArrayLiteralExpression)
    TestCase.assertEquals("'pascalCase'", literal.text)
  }

  fun testResolveIntoComputedProperty() {
    myFixture.configureByText("ResolveIntoComputedProperty.vue", """
<template>
{{<caret>TestRight}}
</template>
<script>
export default {
  name: 'childComp',
  props: {'myMessage': {}},
  computed: {
    testWrong: 111,
    testRight: function() {}
  }
}
</script>""")
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val property = reference!!.resolve()
    TestCase.assertTrue(property is JSProperty)
    TestCase.assertEquals("testRight", (property as JSProperty).name)
  }

  fun testResolveIntoComputedES6FunctionProperty() {
    myFixture.configureByText("ResolveIntoComputedES6FunctionProperty.vue", """
<template>
{{<caret>TestRight}}
</template>
<script>
export default {
  name: 'childComp',
  props: {'myMessage': {}},
  computed: {
    testWrong: 111,
    testRight() {}
  }
}
</script>""")
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val property = reference!!.resolve()
    TestCase.assertTrue(property is JSProperty)
    TestCase.assertEquals("testRight", (property as JSProperty).name)
  }

  fun testResolveIntoMethodsFromBoundAttributes() {
    myFixture.configureByText("child.vue", """
<template>
</template>
<script>
export default {
  name: 'childComp',
  props: {'myMessage': {}},
  methods: {
    reverseMessage() {
      return this.myMessage.reverse()
    }
  }
}
</script>""")
    myFixture.configureByText("ResolveIntoMethodsFromBoundAttributes.vue", """
<template>
    <child-comp v-bind:my-message="me215t<caret>hod"></child-comp>
</template>
<script>
import ChildComp from 'child.vue'
export default {
  components: {ChildComp},
  name: 'parent',
  methods: {
    me215thod: function () {
      return 'something!'
    }
  }
}
</script>""")
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val property = reference!!.resolve()
    TestCase.assertTrue(property is JSProperty)
    TestCase.assertEquals("me215thod", (property as JSProperty).name)
  }

  fun testResolveLocallyInsideComponentPropsArray() {
    myFixture.configureByText("ResolveLocallyInsideComponentPropsArray.vue", """
<script>
export default {
  name: 'parent',
  props: ['parentMsg', 'parentSize'],
  computed: {
    normalizedSize: function () {
      return this.<caret>parentMsg.trim().toLowerCase()
    }
  }
}</script>""")
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val literal = reference!!.resolve()
    TestCase.assertTrue(literal is JSLiteralExpression)
    TestCase.assertTrue((literal as JSLiteralExpression).isQuotedLiteral)
    TestCase.assertEquals("'parentMsg'", literal.text)
  }

  fun testResolveLocallyInsideComponentPropsArrayRefVariant() {
    myFixture.configureByText("ResolveLocallyInsideComponentPropsArrayRefVariant.vue", """
<script>
let props = ['parentMsg', 'parentSize'];
export default {
  name: 'parent',
  props: props,
  computed: {
    normalizedSize: function () {
      return this.<caret>parentMsg.trim().toLowerCase()
    }
  }
}</script>""")
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val literal = reference!!.resolve()
    TestCase.assertTrue(literal is JSLiteralExpression)
    TestCase.assertTrue((literal as JSLiteralExpression).isQuotedLiteral)
    TestCase.assertEquals("'parentMsg'", literal.text)
  }

  fun testResolveLocallyInsideComponentArrayFunctionInsideExport() {
    JSTestUtils.testES6<Exception>(myFixture.project, {
      myFixture.configureByText("ResolveLocallyInsideComponentArrayFunctionInsideExport.vue", """
<script>
let props = ['parentMsg'];

export default {
  name: 'parent',
  props: props,
  methods: {
    oneMethod: () => {
      return this.<caret>parentMsg * 3;
    }
  }
}</script>""")
      val reference = myFixture.getReferenceAtCaretPosition()
      TestCase.assertNotNull(reference)
      val literal = reference!!.resolve()
      TestCase.assertTrue(literal is JSLiteralExpression)
      TestCase.assertTrue((literal as JSLiteralExpression).isQuotedLiteral)
      TestCase.assertEquals("'parentMsg'", literal.text)
    })
  }

  fun testResolveLocallyInsideComponent() {

    doTestResolveLocallyInsideComponent("""
<script>
export default {
  name: 'parent',
  props: {parentMsg: {}, parentSize: {}},
  computed: {
    normalizedSize: function () {
      return this.<caret>parentMsg.trim().toLowerCase()
    }
  }
}</script>
""", "parentMsg")

    doTestResolveLocallyInsideComponent("""
<script>
let props = {parentMsg: {}, parentSize: {}};
export default {
  name: 'parent',
  props: props,
  computed: {
    normalizedSize: function () {
      return this.<caret>parentMsg.trim().toLowerCase()
    }
  }
}</script>
""", "parentMsg")

    doTestResolveLocallyInsideComponent("""
<script>
let props = {parentMsg: {}, parentSize: {}};
export default {
  name: 'parent',
  props: props,
  methods: {
    normalizedSize() {
      return this.<caret>parentMsg.trim().toLowerCase()
    }
  }
}</script>
""", "parentMsg")

    doTestResolveLocallyInsideComponent("""
<script>
let props = {parentMsg: {}, parentSize: {}};
let methods = {
    wise() {
      return this.<caret>normalizedSize() / 2;
    }
  };
let computedProps = {
    normalizedSize() {
      return this.parentMsg.trim().toLowerCase()
    }
  };
export default {
  name: 'parent',
  props: props,
  computedProps: computedProps,
  methods: methods
}</script>
""", "normalizedSize")

    doTestResolveLocallyInsideComponent("""
<script>
let props = {parentMsg: {}, parentSize: {}};

function wouldBeUsedLater() {
  return this.<caret>parentMsg * 3;
}

export default {
  name: 'parent',
  props: props,
}</script>
""", "parentMsg")

    doTestResolveLocallyInsideComponent("""
<script>
let props = ['parentMsg'];

let wouldBeUsedLater = () => {
  return this.<caret>parentMsg * 3;
}

export default {
  name: 'parent',
  props: props,
}</script>
""", null)

    doTestResolveLocallyInsideComponent("""
<template>{{<caret>groceryList}}</template>
<script>
let props = ['parentMsg'];

export default {
  name: 'parent',
  props: props,
  data: {
    groceryList: {}
  }
}</script>
""", "groceryList")

    doTestResolveLocallyInsideComponent("""
<script>
let props = ['parentMsg'];

export default {
  name: 'parent',
  props: props,
  methods: {
    callMum() {
      return this.<caret>groceryList;
    }
  }
  data: {
    groceryList: {}
  }
}</script>
""", "groceryList")

    doTestResolveLocallyInsideComponent("""
<template>{{<caret>groceryList}}</template>
<script>
let props = ['parentMsg'];

export default {
  name: 'parent',
  props: props,
  data: () => ({
    groceryList: {}
  })
}</script>
""", "groceryList")

    doTestResolveLocallyInsideComponent("""
<template>{{<caret>groceryList}}</template>
<script>
let props = ['parentMsg'];

export default {
  name: 'parent',
  props: props,
  data:
    () => {
            return {
              groceryList: {}
            }
          }
}</script>
""", "groceryList")

    doTestResolveLocallyInsideComponent("""
<template>{{groceryList.<caret>carrot}}</template>
<script>
let props = ['parentMsg'];

export default {
  name: 'parent',
  props: props,
  data:
    function () {
            return {
              groceryList: {
                carrot: {}
              }
            }
          }
}</script>
""", "carrot")
}

  fun doTestResolveLocallyInsideComponent(text: String, expectedPropertyName: String?) {
    myFixture.configureByText("ResolveLocallyInsideComponent.vue", text)
    val reference = myFixture.getReferenceAtCaretPosition()
    TestCase.assertNotNull(reference)
    val property = reference!!.resolve()
    if (expectedPropertyName == null) {
      TestCase.assertNull(property)
    } else {
      TestCase.assertTrue(property is JSProperty)
      TestCase.assertEquals(expectedPropertyName, (property as JSProperty).name)
    }
  }
}