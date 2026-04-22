import { Component, Input, OnInit, OnDestroy, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AgentBridgeService } from '../../../services/agent-bridge.service';
import { TransactionResponse, TransactionStepResponse } from '../../../models/AgentResponse.model';
import { Subscription, interval } from 'rxjs';

@Component({
  selector: 'app-agent-progress',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './agent-progress.component.html',
  styleUrls: ['./agent-progress.component.css']
})
export class AgentProgressComponent implements OnInit, OnDestroy {
  @Input() transactionId!: number;
  @Output() progressComplete = new EventEmitter<{success: boolean}>();
  
  transaction?: TransactionResponse;
  loading: boolean = true;
  error?: string;
  private pollSubscription?: Subscription;

  constructor(private bridgeService: AgentBridgeService) {}

  ngOnInit(): void {
    if (this.transactionId) {
      this.loadTransaction();
      // Poll every 2 seconds
      this.pollSubscription = interval(2000).subscribe(() => {
        this.loadTransaction();
      });
    }
  }

  ngOnDestroy(): void {
    if (this.pollSubscription) {
      this.pollSubscription.unsubscribe();
    }
  }

  private loadTransaction() {
    this.bridgeService.getTransactionStatus(this.transactionId).subscribe({
      next: (res) => {
        this.transaction = res;
        this.loading = false;
        
        // Stop polling if transaction is over
        if (['COMPLETED', 'FAILED', 'ROLLED_BACK', 'AWAITING_APPROVAL'].includes(res.status)) {
          if (this.pollSubscription) {
            this.pollSubscription.unsubscribe();
          }
          if (res.status === 'COMPLETED') {
             this.progressComplete.emit({success: true});
          }
        }
      },
      error: (err) => {
        // Just keep old data if poll fails once
        if (this.loading) {
            this.error = "İşlem durumu yüklenemedi.";
            this.loading = false;
        }
      }
    });
  }

  get progressPercentage(): number {
    if (!this.transaction || !this.transaction.totalSteps || this.transaction.totalSteps === 0) return 0;
    return (this.transaction.completedSteps / this.transaction.totalSteps) * 100;
  }
}
