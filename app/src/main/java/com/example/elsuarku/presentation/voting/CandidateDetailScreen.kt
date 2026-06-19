package com.example.elsuarku.presentation.voting

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.elsuarku.presentation.components.CandidateCard
import com.example.elsuarku.presentation.components.LoadingIndicator
import com.example.elsuarku.ui.theme.DeepBlue
import com.example.elsuarku.ui.theme.DeepBlueDark
import com.example.elsuarku.ui.theme.DeepBlueLight
import com.example.elsuarku.ui.theme.DeepBlueSurface
import com.example.elsuarku.ui.theme.EmeraldGreen
import com.example.elsuarku.ui.theme.Gold
import com.example.elsuarku.ui.theme.OnDeepBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CandidateDetailScreen(
    electionId: String,
    viewModel: VotingViewModel,
    onVoteClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.candidateListState.collectAsState()

    LaunchedEffect(electionId) {
        viewModel.loadCandidates(electionId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.election?.title ?: "Kandidat",
                        color = OnDeepBlue,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Kembali", tint = OnDeepBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepBlue)
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LoadingIndicator(message = "Memuat kandidat…")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Election info
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DeepBlueSurface, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = state.election?.description ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DeepBlueDark
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pilih satu kandidat di bawah ini:",
                            style = MaterialTheme.typography.labelMedium,
                            color = DeepBlueLight
                        )
                    }
                }

                // Candidates
                if (state.candidates.isEmpty()) {
                    item {
                        Text(
                            text = "Belum ada kandidat.",
                            color = DeepBlueLight,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    items(state.candidates) { candidate ->
                        CandidateCard(
                            candidate = candidate,
                            onClick = { onVoteClick(candidate.id) }
                        )
                    }
                }
            }
        }
    }
}
