{{- define "mesh-lab-gateway.name" -}}
{{- default .Chart.Name .Values.app.name | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "mesh-lab-gateway.fullname" -}}
{{- printf "%s-%s" .Release.Name (include "mesh-lab-gateway.name" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "mesh-lab-gateway.labels" -}}
app.kubernetes.io/name: {{ include "mesh-lab-gateway.name" . }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version | replace "+" "_" }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/version: {{ .Values.app.version | quote }}
{{- end -}}

{{- define "mesh-lab-gateway.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "mesh-lab-gateway.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end -}}
