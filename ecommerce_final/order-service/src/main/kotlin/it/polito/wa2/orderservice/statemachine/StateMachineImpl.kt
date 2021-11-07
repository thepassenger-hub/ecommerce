package it.polito.wa2.orderservice.statemachine

import it.polito.wa2.orderservice.common.StateMachineEvents
import it.polito.wa2.orderservice.common.StateMachineStates
import it.polito.wa2.orderservice.domain.RedisStateMachine
import it.polito.wa2.orderservice.domain.Transition
import it.polito.wa2.orderservice.dto.ProductDTO
import it.polito.wa2.orderservice.events.SagaFailureEvent
import it.polito.wa2.orderservice.events.SagaFinishedEvent
import it.polito.wa2.orderservice.events.StateMachineEvent
import it.polito.wa2.orderservice.repositories.RedisStateMachineRepository
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.logging.Logger

@Component
@Scope("prototype")
class StateMachineImpl(val initialState: StateMachineStates,
                       val finalState: StateMachineStates,
                       val transitions: MutableList<Transition>,
                       var state: StateMachineStates?,
                       val id: String = "",
                       var failed: Boolean? = false,
                       var completed: Boolean? = false,
                       val customerEmail: String,
                       val shippingAddress: String? = null,
                       val amount: BigDecimal,
                       val products: Set<ProductDTO>? = null,
                       val auth: String,
                       val applicationEventPublisher: ApplicationEventPublisher,
                       val redisStateMachineRepository: RedisStateMachineRepository,
                       val logger: Logger,
                       ) : StateMachine
{

    override suspend fun getTransition(event: StateMachineEvents?): Transition? {
        return if (event != null)
                transitions.find { it.source == state && it.event == event }
            else
                transitions.find { it.source == state }
    }
    override suspend fun start(){
        state = initialState
        getTransition()?.event?.let { this.nextStateAndFireEvent(it) }
    }

    override suspend fun fireEvent(event: ApplicationEvent) {
        applicationEventPublisher.publishEvent(event)
    }

    
    override suspend fun nextStateAndFireEvent(event: StateMachineEvents) {

        val transition = getTransition(event) ?: return

        val oldSM = this.toRedisStateMachine()

        state = transition.target

        failed = if (failed == false && transition.isRollingBack) true else failed


        if (state == finalState){
            fireEvent(SagaFinishedEvent(this))
            completed = true
        } else if (state == initialState){
            fireEvent(SagaFailureEvent(this))
            completed = true
        }

        backup(oldSM, this.toRedisStateMachine())

        fireEvent(StateMachineEvent(this, event ))

        transition.action?.invoke()

    }

    override suspend fun resume(){
        if (this.failed == true)
            return
        else {
            val transition = transitions.find { it.source == state && !it.isRollingBack } ?: return
            if (transition.isPassive)
                return
            else
                this.nextStateAndFireEvent(transition.event!!)
        }
    }

    override suspend fun backup(oldSM: RedisStateMachine, newSM :RedisStateMachine){
        try {
            redisStateMachineRepository.remove(oldSM)
            redisStateMachineRepository.add(newSM)
        } catch (e: Exception) {
            logger.severe(e.toString())
            logger.severe("Cant backup new sm $newSM over old sm $oldSM")
        }
    }

    override fun toString(): String {
        return "[ID: $id, STATE: $state FAILED: $failed, COMPLETED: $completed"
    }
}

fun StateMachineImpl.toRedisStateMachine() = RedisStateMachine(
    initialState,
    state,
    id,
    failed,
    completed,
    customerEmail,
    shippingAddress,
    amount,
    products,
    auth
)