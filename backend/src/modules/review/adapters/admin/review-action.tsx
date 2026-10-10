import React, { useState } from 'react';
import { Box, Button, FormGroup, Label, Text, TextArea } from '@adminjs/design-system';
import { ApiClient, useActionResponseHandler, type ActionProps } from 'adminjs';

const api = new ApiClient();

const ReviewAction: React.FC<ActionProps> = ({ record, resource, action }) => {
  const [note, setNote] = useState('');
  const [loading, setLoading] = useState(false);
  const handleResponse = useActionResponseHandler();
  const rejecting = action.name === 'reject';

  if (!record) return null;

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    setLoading(true);
    try {
      const response = await api.recordAction({
        resourceId: resource.id,
        recordId: record.id,
        actionName: action.name,
        data: { note },
      });
      handleResponse(response.data);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box as="form" onSubmit={submit} variant="container">
      <Text mb="xl">
        {rejecting
          ? 'Laporan akan ditolak dan dikembalikan ke petugas beserta catatan ini.'
          : 'Laporan akan ditandai terverifikasi.'}
      </Text>
      <FormGroup>
        <Label htmlFor="review-note" required={rejecting}>
          Catatan {rejecting ? '' : '(opsional)'}
        </Label>
        <TextArea
          id="review-note"
          width={1}
          rows={4}
          maxLength={500}
          value={note}
          onChange={(event: React.ChangeEvent<HTMLTextAreaElement>) => setNote(event.target.value)}
        />
      </FormGroup>
      <Button type="submit" variant={rejecting ? 'danger' : 'contained'} disabled={loading}>
        {rejecting ? 'Tolak laporan' : 'Verifikasi laporan'}
      </Button>
    </Box>
  );
};

export default ReviewAction;
