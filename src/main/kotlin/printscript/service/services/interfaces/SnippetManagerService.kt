package printscript.service.services.interfaces

import printscript.service.dto.StatusDTO

interface SnippetManagerService {
    fun updateSnippetStatus(newStatus: StatusDTO)
}
