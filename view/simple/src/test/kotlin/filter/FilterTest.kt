package io.holunda.camunda.taskpool.view.simple.filter

import io.holunda.camunda.taskpool.api.task.ProcessReference
import io.holunda.camunda.taskpool.view.DataEntry
import io.holunda.camunda.taskpool.view.Task
import io.holunda.camunda.taskpool.view.TaskWithDataEntries
import io.holunda.camunda.taskpool.view.simple.*
import org.assertj.core.api.Assertions.assertThat
import org.camunda.bpm.engine.variable.Variables
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

class FilterTest {

  @get: Rule
  val expected = ExpectedException.none()

  private val filtersList = listOf("task.name${SEPARATOR}myName", "task.assignee${SEPARATOR}kermit", "dataAttr1${SEPARATOR}value", "dataAttr2${SEPARATOR}another")

  private val ref = ProcessReference("1", "2", "3", "4", "My Process", "myExample")

  // no match: task.assignee, dataAttr1, dataAttr2
  // match: task.name
  private val task1 = TaskWithDataEntries(Task("id", ref, "key", name = "myName"), listOf())
  // no match: task.name, dataAttr1, dataAttr2
  // match: task.assignee
  private val task2 = TaskWithDataEntries(Task("id", ref, "key", assignee = "kermit"), listOf())
  // no match: task.name, task.assignee, dataAttr2
  // match: dataEntries[0].payload -> dataAttr1
  private val task3 = TaskWithDataEntries(Task("id", ref, "key", name = "foo", assignee = "gonzo"), listOf(
    DataEntry("type", "4711", Variables.createVariables().putValue("dataAttr1", "another")
    )))
  // no match: task.name, task.assignee, dataAttr2
  // match: dataEntries[0].payload -> dataAttr1
  private val task4 = TaskWithDataEntries(Task("id", ref, "key", name = "foo", assignee = "gonzo"), listOf(
    DataEntry("type", "4711", Variables.createVariables().putValue("dataAttr1", "value"))
  ))
  // no match: task.name, task.assignee, dataAttr1
  // match: dataEntries[0].payload -> dataAttr2
  private val task5 = TaskWithDataEntries(Task("id", ref, "key", name = "foo", assignee = "gonzo"), listOf(
    DataEntry("type", "4711", Variables.createVariables().putValue("dataAttr2", "another").putValue("name", "myName"))
  ))
  // no match: task.name, task.assignee, dataAttr1
  // match: task.payload -> dataAttr2
  private val task6 = TaskWithDataEntries(Task("id", ref, "key", name = "foo", assignee = "gonzo",
    payload = Variables.createVariables().putValue("dataAttr2", "another").putValue("name", "myName")), listOf())


  @Test
  fun `should classify properties`() {
    assertThat(isTaskAttribute("task.id")).isTrue()
    assertThat(isTaskAttribute("task.name")).isTrue()
    assertThat(isTaskAttribute("task.assignee")).isTrue()

    assertThat(isTaskAttribute("task.")).isFalse()
    assertThat(isTaskAttribute("assignee")).isFalse()
    assertThat(isTaskAttribute("someOther")).isFalse()
    assertThat(isTaskAttribute("described")).isFalse()
  }

  @Test
  fun `should create criteria`() {

    val criteria = toCriteria(filtersList)

    assertThat(criteria).isNotNull
    assertThat(criteria.size).isEqualTo(4)
    assertThat(criteria).containsExactlyElementsOf(listOf(TaskCriterion("name", "myName"), TaskCriterion("assignee", "kermit"),
      DataEntryCriterion("dataAttr1", "value"), DataEntryCriterion("dataAttr2", "another")))
  }

  @Test
  fun `should fail to create criteria`() {
    expected.expect(IllegalArgumentException::class.java)
    toCriteria(listOf("$SEPARATOR$SEPARATOR"))
  }

  @Test
  fun `should fail to create criteria 2`() {
    expected.expect(IllegalArgumentException::class.java)
    toCriteria(listOf("${SEPARATOR}some$SEPARATOR"))
  }

  @Test
  fun `should ignore wrong format`() {
    assertThat(toCriteria(listOf("noSeparator"))).isEmpty()
  }

  @Test
  fun `should create predicates`() {

    val criteria = toCriteria(filtersList)
    val predicates = createPredicates(criteria)

    assertThat(predicates).isNotNull()
    assertThat(predicates.taskPredicate).isNotNull
    assertThat(predicates.dataEntriesPredicate).isNotNull

    assertThat(predicates.taskPredicate!!.test(task1.task)).isTrue()
    assertThat(predicates.taskPredicate!!.test(task2.task)).isTrue()
    assertThat(predicates.taskPredicate!!.test(task3.task)).isFalse()
    assertThat(predicates.dataEntriesPredicate!!.test(task3.dataEntries[0].payload)).isFalse()
    assertThat(predicates.dataEntriesPredicate!!.test(task4.dataEntries[0].payload)).isTrue()
    assertThat(predicates.dataEntriesPredicate!!.test(task5.dataEntries[0].payload)).isTrue()
  }

  @Test
  fun testFilter() {
    val filtered = filter(filtersList, listOf(task1, task2, task3, task4, task5, task6))
    assertThat(filtered).containsExactlyElementsOf(listOf(task1, task2, task4, task5, task6))
  }
}

