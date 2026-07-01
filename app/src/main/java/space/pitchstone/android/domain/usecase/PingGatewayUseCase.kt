package space.pitchstone.android.domain.usecase

import javax.inject.Inject
import space.pitchstone.android.domain.repository.TransactionRepository

class PingGatewayUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(): Result<Boolean> {
        return repository.pingGateway()
    }
}
