package io.holunda.camunda.datapool.core.business

import io.holunda.camunda.taskpool.api.business.*
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.spring.stereotype.Aggregate

@Aggregate
class DataEntryAggregate() {

  @AggregateIdentifier
  private lateinit var dataIdentity: String

  @CommandHandler
  constructor(command: CreateDataEntryCommand) : this() {
    AggregateLifecycle.apply(DataEntryCreatedEvent(
      entryId = command.entryId,
      entryType = command.entryType,
      payload = command.payload,
      correlations = command.correlations
    ))
  }

  @CommandHandler
  fun handle(command: UpdateDataEntryCommand) {
    AggregateLifecycle.apply(DataEntryUpdatedEvent(
      entryId = command.entryId,
      entryType = command.entryType,
      payload = command.payload,
      correlations = command.correlations
    ))
  }

  @EventSourcingHandler
  fun on(event: DataEntryCreatedEvent) {
    this.dataIdentity = dataIdentity(entryType = event.entryType, entryId = event.entryId)
  }

}
