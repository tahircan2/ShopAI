import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AgentBridgeService } from '../../../services/agent-bridge.service';
import { ApprovalResponse } from '../../../models/AgentResponse.model';

@Component({
  selector: 'app-agent-approval-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './agent-approval-card.component.html',
  styleUrls: ['./agent-approval-card.component.css']
})
export class AgentApprovalCardComponent implements OnInit, OnDestroy {
  @Input() approvalToken!: string;
  @Output() actionCompleted = new EventEmitter<{status: string, message: string}>();

  approvalData?: ApprovalResponse;
  loading: boolean = true;
  actionInProgress: boolean = false;
  error?: string;
  parsedPlan: any;
  remainingSeconds: number = 0;
  private countdownInterval: any;

  get estimatedWaitTime(): number {
    if (!this.parsedPlan?.steps) return 5;
    return this.parsedPlan.steps.length * 3 + 2; 
  }

  constructor(private bridgeService: AgentBridgeService) {}

  ngOnInit(): void {
    if (this.approvalToken) {
      this.loadApproval();
    }
  }

  ngOnDestroy(): void {
    this.stopCountdown();
  }

  private loadApproval() {
    this.bridgeService.getApprovalStatus(this.approvalToken).subscribe({
      next: (res) => {
        this.approvalData = res;
        this.remainingSeconds = res.remainingSeconds;
        try {
          this.parsedPlan = JSON.parse(res.planData);
        } catch (e) {
          this.parsedPlan = null;
        }
        this.loading = false;
        if (this.approvalData.status === 'PENDING') {
          this.startCountdown();
        }
      },
      error: (err) => {
        this.error = "Onay verisi yüklenemedi. Süresi dolmuş olabilir.";
        this.loading = false;
      }
    });
  }

  private startCountdown() {
    this.stopCountdown();
    this.countdownInterval = setInterval(() => {
      if (this.remainingSeconds > 0) {
        this.remainingSeconds--;
      } else {
        this.stopCountdown();
        if (this.approvalData?.status === 'PENDING') {
          this.autoReject();
        }
      }
    }, 1000);
  }

  private stopCountdown() {
    if (this.countdownInterval) {
      clearInterval(this.countdownInterval);
    }
  }

  private autoReject() {
    if (this.actionInProgress) return;
    this.reject();
  }

  approve() {
    this.actionInProgress = true;
    this.bridgeService.approveAction(this.approvalToken).subscribe({
      next: (res) => {
        this.actionInProgress = false;
        this.stopCountdown();
        if(this.approvalData) this.approvalData.status = 'APPROVED';
        this.actionCompleted.emit({status: 'APPROVED', message: res.message});
      },
      error: (err) => {
        this.actionInProgress = false;
        this.error = 'Onay sırasında bir hata oluştu.';
      }
    });
  }

  reject() {
    this.actionInProgress = true;
    this.bridgeService.rejectAction(this.approvalToken).subscribe({
      next: (res) => {
        this.actionInProgress = false;
        this.stopCountdown();
        if(this.approvalData) this.approvalData.status = 'REJECTED';
        this.actionCompleted.emit({status: 'REJECTED', message: res.message});
      },
      error: (err) => {
        this.actionInProgress = false;
        this.error = 'Reddedilirken bir hata oluştu.';
      }
    });
  }
}
